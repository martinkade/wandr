package com.wandr.android.ui.profile.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/** Join a group by typing its invite code or scanning the QR code. */
@Composable
fun JoinTeamDialog(
    code: String,
    onCodeChange: (String) -> Unit,
    isJoining: Boolean,
    errorMessage: String?,
    onScan: () -> Unit,
    onJoin: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isJoining) onDismiss() },
        title = { Text(stringResource(R.string.join_team_title)) },
        text = {
            Column {
                Text(stringResource(R.string.join_team_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    label = { Text(stringResource(R.string.join_team_code_label)) },
                    singleLine = true,
                    enabled = !isJoining,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                OutlinedButton(onClick = onScan, enabled = !isJoining, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.join_team_scan))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onJoin, enabled = code.isNotBlank() && !isJoining) {
                Text(stringResource(R.string.join_team_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isJoining) { Text(stringResource(R.string.cancel_button)) }
        }
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 900)
@Composable
private fun JoinTeamDialogPreview() {
    WandrTheme {
        JoinTeamDialog("X7K9", {}, isJoining = false, errorMessage = null, onScan = {}, onJoin = {}, onDismiss = {})
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun JoinTeamDialogErrorPreview() {
    WandrTheme {
        JoinTeamDialog("NOPE", {}, isJoining = false, errorMessage = "Invalid invite code", onScan = {}, onJoin = {}, onDismiss = {})
    }
}
