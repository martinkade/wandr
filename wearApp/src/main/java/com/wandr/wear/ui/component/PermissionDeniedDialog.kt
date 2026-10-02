package com.wandr.wear.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Text
import com.wandr.wear.R
import com.wandr.wear.ui.WearPreviews
import com.wandr.wear.ui.theme.WandrWearTheme

/** Shown when the user denied a permission: offers the settings as fallback. */
@Composable
fun PermissionDeniedDialog(visible: Boolean, onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        confirmButton = { AlertDialogDefaults.ConfirmButton(onClick = onOpenSettings) },
        dismissButton = { AlertDialogDefaults.DismissButton(onClick = onDismiss) },
        title = { Text(stringResource(R.string.perm_denied_title)) },
        text = { Text(stringResource(R.string.perm_denied_message)) }
    )
}

@WearPreviews
@Composable
private fun PermissionDeniedDialogPreview() {
    WandrWearTheme { PermissionDeniedDialog(visible = true, onOpenSettings = {}, onDismiss = {}) }
}
