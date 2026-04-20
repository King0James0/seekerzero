package dev.seekerzero.app.ui.theme

import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

object SeekerZeroColors {
    val Background = Color(0xFF0B0D10)
    val Surface = Color(0xFF14171C)
    val SurfaceVariant = Color(0xFF1C2026)
    val CardBorder = Color(0xFF2A2F36)
    val Divider = Color(0xFF23272E)

    val Primary = Color(0xFF4C9EFF)
    val OnPrimary = Color(0xFF08111A)

    val Accent = Color(0xFF6CE3C4)
    val Warning = Color(0xFFE7B24A)
    val Error = Color(0xFFE5484D)
    val Success = Color(0xFF5BD48A)

    val TextPrimary = Color(0xFFE6E8EB)
    val TextSecondary = Color(0xFF8A929C)
    val TextDisabled = Color(0xFF565C65)
}

@Composable
@ReadOnlyComposable
fun seekerZeroSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = SeekerZeroColors.OnPrimary,
    checkedTrackColor = SeekerZeroColors.Primary,
    checkedBorderColor = SeekerZeroColors.Primary,
    uncheckedThumbColor = SeekerZeroColors.TextSecondary,
    uncheckedTrackColor = SeekerZeroColors.Surface,
    uncheckedBorderColor = SeekerZeroColors.CardBorder,
    disabledCheckedThumbColor = SeekerZeroColors.TextDisabled,
    disabledCheckedTrackColor = SeekerZeroColors.SurfaceVariant,
    disabledUncheckedThumbColor = SeekerZeroColors.TextDisabled,
    disabledUncheckedTrackColor = SeekerZeroColors.SurfaceVariant
)
