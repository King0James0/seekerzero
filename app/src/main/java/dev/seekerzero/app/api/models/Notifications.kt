package dev.seekerzero.app.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class NotificationItem(
    val id: Long,
    @SerialName("created_at_ms") val createdAtMs: Long,
    val title: String,
    val body: String,
    @SerialName("deep_link") val deepLink: String? = null,
    val payload: JsonObject? = null,
    @SerialName("bell_read") val bellRead: Boolean = false,
    @SerialName("read_at_ms") val readAtMs: Long? = null,
)

@Serializable
data class NotificationsListResponse(
    val ok: Boolean,
    val items: List<NotificationItem> = emptyList(),
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable
data class NotificationsMarkReadRequest(
    val ids: List<Long>? = null,
    val all: Boolean? = null,
)

@Serializable
data class NotificationsMarkReadResponse(
    val ok: Boolean,
    val marked: Int = 0,
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable
data class NotificationsUnreadCountResponse(
    val ok: Boolean,
    val count: Int = 0,
)

@Serializable
data class NotificationsDeleteRequest(
    val ids: List<Long>? = null,
    val all: Boolean? = null,
)

@Serializable
data class NotificationsDeleteResponse(
    val ok: Boolean,
    val deleted: Int = 0,
    @SerialName("unread_count") val unreadCount: Int = 0,
)
