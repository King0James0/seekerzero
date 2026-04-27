package dev.seekerzero.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.seekerzero.app.api.models.NotificationItem
import dev.seekerzero.app.notifications.NotificationsRepository
import dev.seekerzero.app.ui.components.CardSurface
import dev.seekerzero.app.ui.components.SeekerZeroScaffold
import dev.seekerzero.app.ui.theme.SeekerZeroColors
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen notifications modal. Refreshes once on attach, then refreshes
 * again on dismiss after marking all read so the bell badge clears.
 */
@Composable
fun NotificationsDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val items by NotificationsRepository.items.collectAsStateWithLifecycle()
    val unread by NotificationsRepository.unreadCount.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { NotificationsRepository.refresh() }

    DisposableEffect(Unit) {
        onDispose {
            // Mark everything read on close. Fire-and-forget; the next time
            // the bell observes the repo it will see unread=0.
            scope.launch { NotificationsRepository.markAllRead() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SeekerZeroScaffold(
            title = if (unread > 0) "Notifications · $unread" else "Notifications",
            onBack = onDismiss,
            actions = {
                if (unread > 0) {
                    TextButton(onClick = {
                        scope.launch { NotificationsRepository.markAllRead() }
                    }) {
                        Text(
                            text = "Mark all read",
                            color = SeekerZeroColors.Primary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        ) { pad ->
            Box(modifier = Modifier.fillMaxSize().padding(pad)) {
                if (items.isEmpty()) {
                    EmptyState()
                } else {
                    NotificationList(
                        items,
                        onItemTap = { id ->
                            scope.launch { NotificationsRepository.markRead(listOf(id)) }
                        },
                        onItemDismiss = { id ->
                            scope.launch { NotificationsRepository.delete(listOf(id)) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationList(
    items: List<NotificationItem>,
    onItemTap: (Long) -> Unit,
    onItemDismiss: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            // `key` ensures the SwipeToDismiss state resets when a row's
            // identity changes (e.g. an id is reused after a deletion).
            key(item.id) {
                SwipeableNotificationRow(
                    item = item,
                    onTap = { onItemTap(item.id) },
                    onDismiss = { onItemDismiss(item.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationRow(
    item: NotificationItem,
    onTap: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd ||
                value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else false
        },
        // Require swiping ~40% of the row width before commit. Default is
        // 56dp which feels too touchy for a notification stack.
        positionalThreshold = { distance -> distance * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val align = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SeekerZeroColors.Error.copy(alpha = 0.85f))
                    .padding(horizontal = 20.dp),
                contentAlignment = align
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "delete",
                    tint = SeekerZeroColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        NotificationRow(item = item, onTap = onTap)
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, onTap: () -> Unit) {
    val source = (item.payload?.get("source") as? JsonPrimitive)?.content ?: "scheduler"
    val type = (item.payload?.get("type") as? JsonPrimitive)?.content ?: ""
    val accent = colorForType(type, source)
    val unread = !item.bellRead

    CardSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onTap)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Type-accent dot, doubles as unread marker (full-color when
            // unread, dim when read).
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (unread) accent else SeekerZeroColors.TextDisabled)
            )
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title.ifBlank { "Agent Zero" },
                        color = SeekerZeroColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatRelative(item.createdAtMs),
                        color = SeekerZeroColors.TextSecondary,
                        fontSize = 10.sp
                    )
                }
                if (item.body.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    LinkifiedBody(text = item.body, onTextTap = onTap)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = source,
                    color = SeekerZeroColors.TextDisabled,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (unread) {
                IconButton(
                    onClick = onTap,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Done,
                        contentDescription = "mark read",
                        tint = SeekerZeroColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No notifications yet",
                color = SeekerZeroColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "notify_user calls and scheduled deliveries land here",
                color = SeekerZeroColors.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Renders a notification body, turning any http(s) URL into a tappable
 * underlined span. Tap on a URL → ACTION_VIEW (system browser). Tap on
 * non-link text → propagate to [onTextTap] so the row's "mark read"
 * behavior keeps working when the user taps inside the body.
 */
@Composable
private fun LinkifiedBody(text: String, onTextTap: () -> Unit) {
    val matches = URL_REGEX.findAll(text).toList()
    if (matches.isEmpty()) {
        Text(text = text, color = SeekerZeroColors.TextSecondary, fontSize = 12.sp)
        return
    }
    val context = LocalContext.current
    val annotated = buildAnnotatedString {
        var cursor = 0
        for (m in matches) {
            if (m.range.first > cursor) append(text.substring(cursor, m.range.first))
            val url = m.value.trimEnd('.', ',', ')', ']', '!', '?', ';', ':')
            pushStringAnnotation(tag = "url", annotation = url)
            withStyle(
                SpanStyle(
                    color = SeekerZeroColors.Primary,
                    textDecoration = TextDecoration.Underline
                )
            ) { append(url) }
            pop()
            cursor = m.range.first + url.length
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
    ClickableText(
        text = annotated,
        style = TextStyle(color = SeekerZeroColors.TextSecondary, fontSize = 12.sp),
        onClick = { offset ->
            val link = annotated
                .getStringAnnotations(tag = "url", start = offset, end = offset)
                .firstOrNull()
            if (link != null) {
                openUrl(context, link.item)
            } else {
                onTextTap()
            }
        }
    )
}

private fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (t: Throwable) {
        Toast.makeText(context, "No app can open $url", Toast.LENGTH_LONG).show()
    }
}

private val URL_REGEX = Regex("""https?://[^\s<>"']+""")

private fun colorForType(type: String, source: String): androidx.compose.ui.graphics.Color = when {
    type.equals("error", true) -> SeekerZeroColors.Error
    type.equals("warning", true) -> SeekerZeroColors.Primary
    type.equals("success", true) -> SeekerZeroColors.Primary
    source == "webui_bell" -> SeekerZeroColors.Primary
    else -> SeekerZeroColors.TextSecondary
}

private fun formatRelative(ms: Long): String {
    if (ms <= 0L) return ""
    val now = System.currentTimeMillis()
    val diff = (now - ms) / 1000
    return when {
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        diff < 604800 -> "${diff / 86400}d ago"
        else -> SimpleDateFormat("MMM d", Locale.US).format(Date(ms))
    }
}
