package com.wandr.android.ui.permission

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.R

/** Explains why a permission is needed. Shown BEFORE the system permission dialog. */
@Composable
fun PermissionDisclosureDialog(
    title: String,
    message: String,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onContinue) { Text(stringResource(R.string.permission_continue)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.permission_not_now)) } }
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun PermissionDisclosureDialogPreview() {
    WandrTheme {
        PermissionDisclosureDialog(
            title = "Location access",
            message = "WANDR uses your location to record the route and distance of your activity.",
            onContinue = {},
            onDismiss = {}
        )
    }
}
