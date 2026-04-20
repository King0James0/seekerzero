package dev.seekerzero.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.seekerzero.app.ui.theme.SeekerZeroColors
import dev.seekerzero.app.ui.theme.SeekerZeroTheme

@Composable
fun InfoDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = SeekerZeroColors.TextPrimary) },
        text = { Text(body, color = SeekerZeroColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = SeekerZeroColors.Primary)
            }
        },
        containerColor = SeekerZeroColors.Surface
    )
}

@Preview
@Composable
private fun InfoDialogPreview() {
    SeekerZeroTheme {
        InfoDialog(
            title = "Tailnet host",
            body = "The MagicDNS name of your Agent Zero server on the tailnet.",
            onDismiss = {}
        )
    }
}
