package dev.seekerzero.app.notifications

import dev.seekerzero.app.api.MobileApiClient
import dev.seekerzero.app.api.models.NotificationItem
import dev.seekerzero.app.util.LogCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for the in-app notification bell.
 *
 * Backed by the server-side `push_queue` table via the mobile/notifications
 * endpoints. That table is also the source of system-notification
 * deliveries, so the bell history and the Android-tray notifications stay
 * in sync.
 *
 * Concurrent refreshes (foreground transition + service-side push wake-up
 * arriving simultaneously) are serialized through `mutex`; the StateFlow
 * is the only thing the UI reads.
 */
object NotificationsRepository {
    private const val TAG = "NotificationsRepo"
    private const val LIST_LIMIT = 100

    private val mutex = Mutex()

    private val _items = MutableStateFlow<List<NotificationItem>>(emptyList())
    val items: StateFlow<List<NotificationItem>> = _items.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // Global overlay state. The dialog lives at MainScaffold level (shared
    // across tabs) and any caller can open/close it: the BellAction icon,
    // a notification deep-link, etc.
    private val _overlayOpen = MutableStateFlow(false)
    val overlayOpen: StateFlow<Boolean> = _overlayOpen.asStateFlow()

    fun openOverlay() { _overlayOpen.value = true }
    fun closeOverlay() { _overlayOpen.value = false }

    suspend fun refresh(): Result<Unit> = mutex.withLock {
        MobileApiClient.notificationsList(sinceMs = 0L, limit = LIST_LIMIT)
            .onSuccess { resp ->
                _items.value = resp.items
                _unreadCount.value = resp.unreadCount
            }
            .map { Unit }
    }

    suspend fun markAllRead(): Result<Unit> = mutex.withLock {
        MobileApiClient.notificationsMarkRead(all = true)
            .onSuccess { resp ->
                _unreadCount.value = resp.unreadCount
                // Reflect the read flag locally without a full refetch.
                _items.value = _items.value.map { it.copy(bellRead = true) }
            }
            .map { Unit }
    }

    suspend fun markRead(ids: List<Long>): Result<Unit> = mutex.withLock {
        if (ids.isEmpty()) return@withLock Result.success(Unit)
        MobileApiClient.notificationsMarkRead(ids = ids)
            .onSuccess { resp ->
                _unreadCount.value = resp.unreadCount
                val read = ids.toHashSet()
                _items.value = _items.value.map {
                    if (it.id in read) it.copy(bellRead = true) else it
                }
            }
            .map { Unit }
    }

    /**
     * Delete bell rows server-side. Updates the local list optimistically
     * so swipe-to-dismiss feels instant; on failure, the next refresh
     * pulls them back. Use ids=null + all=true to clear everything.
     */
    suspend fun delete(ids: List<Long>): Result<Unit> = mutex.withLock {
        if (ids.isEmpty()) return@withLock Result.success(Unit)
        val gone = ids.toHashSet()
        // Optimistic local removal.
        val before = _items.value
        _items.value = before.filterNot { it.id in gone }
        _unreadCount.value = _items.value.count { !it.bellRead }
        return@withLock MobileApiClient.notificationsDelete(ids = ids)
            .onSuccess { resp -> _unreadCount.value = resp.unreadCount }
            .onFailure {
                // Revert on failure so the user sees the row again.
                _items.value = before
                _unreadCount.value = before.count { !it.bellRead }
            }
            .map { Unit }
    }

    /**
     * Called by SeekerZeroService.startPushPoller on each long-poll batch so
     * the bell's unread count + list reflect new arrivals without waiting
     * for the user to open the app modal. Best-effort; failures logged.
     */
    suspend fun onPushArrived() {
        refresh().onFailure {
            LogCollector.w(TAG, "onPushArrived refresh failed: ${it.message}")
        }
    }
}
