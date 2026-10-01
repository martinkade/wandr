package com.wandr.android.ui.profile

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wandr.android.R

@Composable
fun AvatarSourceDialog(
    canTakePhoto: Boolean,
    canRemove: Boolean,
    onChooseFromLibrary: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.avatar_change_title)) },
        text = {
            Column {
                TextButton(onClick = onChooseFromLibrary) { Text(stringResource(R.string.avatar_choose_library)) }
                if (canTakePhoto) {
                    TextButton(onClick = onTakePhoto) { Text(stringResource(R.string.avatar_take_photo)) }
                }
                if (canRemove) {
                    TextButton(onClick = onRemove) {
                        Text(stringResource(R.string.avatar_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_button)) } }
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun AvatarSourceDialogPreview() {
    MaterialTheme {
        AvatarSourceDialog(
            canTakePhoto = true, canRemove = true,
            onChooseFromLibrary = {}, onTakePhoto = {}, onRemove = {}, onDismiss = {}
        )
    }
}
