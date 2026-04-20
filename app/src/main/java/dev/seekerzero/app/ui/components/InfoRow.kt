package dev.seekerzero.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.seekerzero.app.ui.theme.SeekerZeroColors
import dev.seekerzero.app.ui.theme.SeekerZeroTheme

@Composable
fun InfoRow(
    label: String,
    value: String,
    dotColor: Color? = null,
    isLast: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dotColor != null) {
                    StatusDot(color = dotColor)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(label, color = SeekerZeroColors.TextPrimary)
            }
            Text(value, color = SeekerZeroColors.TextSecondary)
        }
        if (!isLast) {
            HorizontalDivider(color = SeekerZeroColors.Divider, thickness = 1.dp)
        } else {
            Spacer(modifier = Modifier.height(0.dp))
        }
    }
}

@Preview
@Composable
private fun InfoRowPreview() {
    SeekerZeroTheme {
        Column {
            InfoRow(label = "Connection", value = "Connected", dotColor = SeekerZeroColors.Success)
            InfoRow(label = "Last contact", value = "3s ago", isLast = true)
        }
    }
}
