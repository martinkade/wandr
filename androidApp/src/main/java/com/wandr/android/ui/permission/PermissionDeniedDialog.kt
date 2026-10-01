package com.wandr.android.ui.permission

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wandr.android.R

/** Shown when the system no longer asks for a permission; leads the user to the app settings. */
@Composable
fun PermissionDeniedDialog(
    message: String,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.permission_denied_title)) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.permission_open_settings)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) } }
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun PermissionDeniedDialogPreview() {
    MaterialTheme {
        PermissionDeniedDialog(
            message = "Location access is turned off. Enable it in the settings to record activities.",
            onOpenSettings = {},
            onDismiss = {}
        )
    }
}
