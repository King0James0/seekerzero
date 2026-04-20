package dev.seekerzero.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.seekerzero.app.ui.theme.SeekerZeroColors
import dev.seekerzero.app.ui.theme.SeekerZeroTheme

@Composable
fun StatusDot(
    color: Color,
    size: Dp = 10.dp,
    pulsing: Boolean = false
) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "status-dot")
        transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "status-dot-alpha"
        ).value
    } else {
        1.0f
    }
    Row {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(size)
                .alpha(alpha)
                .background(color, CircleShape)
        )
    }
}

@Preview
@Composable
private fun StatusDotPreview() {
    SeekerZeroTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusDot(color = SeekerZeroColors.Success)
            StatusDot(color = SeekerZeroColors.Warning)
            StatusDot(color = SeekerZeroColors.Error, pulsing = true)
        }
    }
}
