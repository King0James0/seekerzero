package dev.seekerzero.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.seekerzero.app.ui.theme.SeekerZeroColors
import dev.seekerzero.app.ui.theme.SeekerZeroTheme
import dev.seekerzero.app.ui.theme.seekerZeroSwitchColors

@Composable
fun SettingRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    info: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = SeekerZeroColors.TextPrimary)
            if (info != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = info) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = SeekerZeroColors.TextSecondary
                    )
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = seekerZeroSwitchColors()
        )
    }
}

@Preview
@Composable
private fun SettingRowPreview() {
    SeekerZeroTheme {
        SettingRow(label = "Connect on boot", checked = true, onChange = {}, info = {})
    }
}
