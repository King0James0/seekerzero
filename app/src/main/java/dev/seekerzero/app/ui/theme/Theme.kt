package dev.seekerzero.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SeekerZeroColors.Primary,
    onPrimary = SeekerZeroColors.OnPrimary,
    secondary = SeekerZeroColors.Accent,
    onSecondary = SeekerZeroColors.OnPrimary,
    tertiary = SeekerZeroColors.Warning,
    background = SeekerZeroColors.Background,
    onBackground = SeekerZeroColors.TextPrimary,
    surface = SeekerZeroColors.Surface,
    onSurface = SeekerZeroColors.TextPrimary,
    surfaceVariant = SeekerZeroColors.SurfaceVariant,
    onSurfaceVariant = SeekerZeroColors.TextSecondary,
    error = SeekerZeroColors.Error,
    onError = SeekerZeroColors.OnPrimary,
    outline = SeekerZeroColors.CardBorder,
    outlineVariant = SeekerZeroColors.Divider
)

@Composable
fun SeekerZeroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = SeekerZeroTypography,
        content = content
    )
}
