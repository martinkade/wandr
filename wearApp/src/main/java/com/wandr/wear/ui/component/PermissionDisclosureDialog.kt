package com.wandr.wear.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Text
import com.wandr.wear.R
import com.wandr.wear.ui.WearPreviews
import com.wandr.wear.ui.theme.WandrWearTheme

/** Explains why the permissions are needed. Shown before the system prompt (project rule "Permission Disclosure"). */
@Composable
fun PermissionDisclosureDialog(visible: Boolean, onContinue: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        visible = visible,
        onDismissRequest = onCancel,
        confirmButton = { AlertDialogDefaults.ConfirmButton(onClick = onContinue) },
        dismissButton = { AlertDialogDefaults.DismissButton(onClick = onCancel) },
        title = { Text(stringResource(R.string.perm_disclosure_title)) },
        text = { Text(stringResource(R.string.perm_disclosure_message)) }
    )
}

@WearPreviews
@Composable
private fun PermissionDisclosureDialogPreview() {
    WandrWearTheme { PermissionDisclosureDialog(visible = true, onContinue = {}, onCancel = {}) }
}
