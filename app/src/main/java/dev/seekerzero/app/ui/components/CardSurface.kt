package dev.seekerzero.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import dev.seekerzero.app.ui.theme.SeekerZeroColors
import dev.seekerzero.app.ui.theme.SeekerZeroShapes
import dev.seekerzero.app.ui.theme.SeekerZeroTheme

@Composable
fun CardSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(SeekerZeroShapes.Card)
            .background(SeekerZeroColors.Surface)
            .border(1.dp, SeekerZeroColors.CardBorder, SeekerZeroShapes.Card)
    ) {
        content()
    }
}

@Preview
@Composable
private fun CardSurfacePreview() {
    SeekerZeroTheme {
        CardSurface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Box(modifier = Modifier.padding(16.dp)) {
                Text("Card content — caller provides padding", color = SeekerZeroColors.TextPrimary)
            }
        }
    }
}
