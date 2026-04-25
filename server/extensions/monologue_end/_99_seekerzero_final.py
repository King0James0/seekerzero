# SeekerZero mobile chat: final-event emitter + JSONL mirror.
#
# Hooks A0's monologue_end, which fires after a full agent turn completes
# (i.e. after the "response" tool has been invoked and the monologue exits).
# At this point we take the pre-allocated assistant_message_id from the
# context's chat bus, read the final response text from the context log,
# write a row to the mobile JSONL mirror, publish a `final` event, and
# clear the busy flag.
#
# Two flows are supported:
#   1. Phone-initiated: bus['turn_state'] was set by _chat_dispatch_to_a0
#      when the phone called /mobile/chat/send. We use the pre-allocated
#      assistant_id and emit the final.
#   2. WebUI-initiated on a mobile context: turn_state is None (the WebUI
#      doesn't go through our send view). We bootstrap fresh ids, pull the
#      most recent type="user" log item (added by mq.log_user_message),
#      mirror both user + assistant entries, and publish both events so an
#      active phone subscriber catches up.
#
# Shared state with the Flask handler lives on context.data[CHAT_BUS_KEY] —
# see the forwarder extension's header for the why.
#
# Only fires for the mobile-* contexts and only for the top-level
# agent (subordinate turns also call monologue_end, and we don't want to
# terminate the user-visible reply on a subordinate's completion).

import json
import queue
import time
import uuid
from pathlib import Path

from agent import LoopData
from python.helpers.extension import Extension


_MOBILE_CONTEXT_PREFIX = 'mobile-'
_CHAT_BUS_KEY = '_seekerzero_chat_bus'
_CHAT_DIR = Path('/a0/usr/seekerzero/chat')


def _extract_final_text(agent) -> str:
    """Pull the final assistant text from the most recent response log item,
    defensively expand any surviving §§include(...) references, then strip
    A0's task-stats footer so the phone only sees the actual reply."""
    try:
        logs = agent.context.log.logs
        for item in reversed(logs):
            if getattr(item, 'type', None) == 'response':
                content = getattr(item, 'content', '')
                if isinstance(content, str) and content:
                    expanded = _safe_expand_includes(content)
                    return _strip_task_stats_footer(expanded)
    except Exception:
        pass
    return ''


def _extract_latest_user_message(agent):
    """Return (text, created_at_ms) for the most recent type='user' log
    item, or (None, None) if absent. Used by the WebUI-bootstrap branch."""
    try:
        logs = agent.context.log.logs
        for item in reversed(logs):
            if getattr(item, 'type', None) == 'user':
                content = getattr(item, 'content', '')
                if isinstance(content, str):
                    ts = getattr(item, 'timestamp', None)
                    ts_ms = int(ts * 1000) if ts else None
                    return content, ts_ms
    except Exception:
        pass
    return None, None


def _strip_task_stats_footer(text: str) -> str:
    """Remove A0's task-stats footer. The footer is appended by the
    _05_task_stats_display monologue_end extension in the form:

        <assistant reply text>

        ---
        ⏱ Task completed in 4.8s
        <cost table, cache line, daily budget line>

    Match the unique footer fingerprint: a "---" line whose next
    non-blank line starts with "⏱ Task completed". Scan from the end
    so the real footer (always last) wins even on the off chance the
    reply itself ends with that phrase."""
    if not text:
        return text
    lines = text.splitlines()
    for i in range(len(lines) - 1, 0, -1):
        if lines[i].strip() != '---':
            continue
        j = i + 1
        while j < len(lines) and not lines[j].strip():
            j += 1
        if j < len(lines) and lines[j].lstrip().startswith('⏱ Task completed'):
            return '\n'.join(lines[:i]).rstrip()
    return text


def _safe_expand_includes(text: str) -> str:
    try:
        from python.helpers.strings import replace_file_includes
        return replace_file_includes(text)
    except Exception:
        return text


def _append_mirror(context_id: str, record: dict) -> None:
    try:
        _CHAT_DIR.mkdir(parents=True, exist_ok=True)
        path = _CHAT_DIR / f'{context_id}.jsonl'
        with path.open('a', encoding='utf-8') as f:
            f.write(json.dumps(record, ensure_ascii=False) + '\n')
    except OSError:
        pass  # mirror is best-effort; A0's own chat.json is the durable record


def _publish(bus, event: dict) -> None:
    with bus['subs_lock']:
        subs = list(bus['subscribers'])
    for q in subs:
        try:
            q.put_nowait(event)
        except queue.Full:
            pass


class SeekerzeroFinal(Extension):

    async def execute(self, loop_data: LoopData = LoopData(), **kwargs):
        ctx_id = self.agent.context.id
        if not isinstance(ctx_id, str) or not ctx_id.startswith(_MOBILE_CONTEXT_PREFIX):
            return
        if getattr(self.agent, 'number', 0) != 0:
            return

        bus = self.agent.context.data.get(_CHAT_BUS_KEY)
        if not bus:
            # WebUI started the very first turn on this mobile context
            # before any phone activity touched it. Lazily bootstrap a
            # bus so we can mirror + publish for late-joining subscribers.
            try:
                from python.api.seekerzero_mobile_api import get_chat_bus
                bus = get_chat_bus(ctx_id)
            except Exception:
                return

        with bus['turn_lock']:
            state = bus['turn_state']
            bus['turn_state'] = None

        final_text = _extract_final_text(self.agent)
        now_ms = int(time.time() * 1000)

        if state:
            # Phone-initiated turn: assistant_id was pre-allocated.
            assistant_id = state.get('assistant_id')
            if not assistant_id:
                with bus['busy_lock']:
                    bus['busy'] = False
                return
            _append_mirror(ctx_id, {
                'id': assistant_id,
                'role': 'assistant',
                'content': final_text,
                'created_at_ms': now_ms,
                'is_final': True,
            })
            _publish(bus, {
                'type': 'final',
                'message_id': assistant_id,
                'role': 'assistant',
                'content': final_text,
                'created_at_ms': now_ms,
            })
            with bus['busy_lock']:
                bus['busy'] = False
            return

        # WebUI-initiated turn on a mobile context. Bootstrap user + assistant.
        user_text, user_ts_ms = _extract_latest_user_message(self.agent)
        if not user_text and not final_text:
            return

        user_id = f'msg-u-{uuid.uuid4().hex[:12]}'
        assistant_id = f'msg-a-{uuid.uuid4().hex[:12]}'
        user_ts_ms = user_ts_ms or (now_ms - 1)

        if user_text:
            user_record = {
                'id': user_id,
                'role': 'user',
                'content': user_text,
                'created_at_ms': user_ts_ms,
                'is_final': True,
            }
            _append_mirror(ctx_id, user_record)
            _publish(bus, {
                'type': 'user_msg',
                'message_id': user_id,
                'role': 'user',
                'content': user_text,
                'created_at_ms': user_ts_ms,
            })

        _append_mirror(ctx_id, {
            'id': assistant_id,
            'role': 'assistant',
            'content': final_text,
            'created_at_ms': now_ms,
            'is_final': True,
        })
        _publish(bus, {
            'type': 'final',
            'message_id': assistant_id,
            'role': 'assistant',
            'content': final_text,
            'created_at_ms': now_ms,
        })
