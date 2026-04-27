package dev.seekerzero.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.seekerzero.app.notifications.NotificationsRepository
import dev.seekerzero.app.ui.theme.SeekerZeroColors

/**
 * Bell icon with an unread badge. Drop into a TopAppBar's `actions` slot.
 * Reads `NotificationsRepository.unreadCount`; tapping opens the global
 * overlay state, which MainScaffold renders as the notifications dialog.
 */
@Composable
fun BellAction() {
    val unread by NotificationsRepository.unreadCount.collectAsStateWithLifecycle()

    Box(modifier = Modifier.padding(end = 4.dp)) {
        IconButton(onClick = { NotificationsRepository.openOverlay() }) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = SeekerZeroColors.TextPrimary
            )
        }
        if (unread > 0) {
            val label = if (unread > 99) "99+" else unread.toString()
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .widthIn(min = 16.dp)
                    .size(width = 18.dp, height = 16.dp)
                    .clip(if (unread > 9) RoundedCornerShape(8.dp) else CircleShape)
                    .background(SeekerZeroColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = SeekerZeroColors.Background,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
