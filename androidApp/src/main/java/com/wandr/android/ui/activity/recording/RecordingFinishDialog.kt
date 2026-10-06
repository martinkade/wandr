package com.wandr.android.ui.activity.recording

import android.content.res.Configuration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/** Asked when the user finishes: save the activity, throw it away, or keep recording. */
@Composable
fun RecordingFinishDialog(onSave: () -> Unit, onDiscard: () -> Unit, onContinue: () -> Unit) {
    AlertDialog(
        onDismissRequest = onContinue,
        title = { Text(stringResource(R.string.recording_finish_title)) },
        text = { Text(stringResource(R.string.recording_finish_message)) },
        confirmButton = { TextButton(onClick = onSave) { Text(stringResource(R.string.recording_save_button)) } },
        dismissButton = {
            TextButton(onClick = onDiscard) { Text(stringResource(R.string.recording_discard_button)) }
            TextButton(onClick = onContinue) { Text(stringResource(R.string.recording_continue_button)) }
        }
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 700)
@Composable
private fun RecordingFinishDialogPreview() {
    WandrTheme { RecordingFinishDialog(onSave = {}, onDiscard = {}, onContinue = {}) }
}
