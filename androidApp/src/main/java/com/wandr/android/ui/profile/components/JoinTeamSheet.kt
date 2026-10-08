package com.wandr.android.ui.profile.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/** Bottom sheet to join a team by typing its invite code or scanning the QR code. It cannot be dismissed while joining. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinTeamSheet(
    code: String,
    onCodeChange: (String) -> Unit,
    isJoining: Boolean,
    errorMessage: String?,
    onScan: () -> Unit,
    onJoin: () -> Unit,
    onDismiss: () -> Unit
) {
    val joining by rememberUpdatedState(isJoining)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { !joining } // no swipe-away while joining
        )
    ) {
        JoinTeamSheetContent(code, onCodeChange, isJoining, errorMessage, onScan, onJoin)
    }
}

@Composable
internal fun JoinTeamSheetContent(
    code: String,
    onCodeChange: (String) -> Unit,
    isJoining: Boolean,
    errorMessage: String?,
    onScan: () -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
    ) {
        Text(stringResource(R.string.join_team_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.join_team_hint),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            label = { Text(stringResource(R.string.join_team_code_label)) },
            singleLine = true,
            enabled = !isJoining,
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
        Button(
            onClick = onJoin,
            enabled = code.isNotBlank() && !isJoining,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) { Text(stringResource(R.string.join_team_button)) }

        Text(
            stringResource(R.string.join_team_or),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedButton(
            onClick = onScan,
            enabled = !isJoining,
            modifier = Modifier
                .fillMaxWidth()
        ) { Text(stringResource(R.string.join_team_scan)) }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 900)
@Composable
private fun JoinTeamSheetPreview() {
    WandrTheme {
        Surface {
            JoinTeamSheetContent(
                "X7K9",
                {},
                isJoining = false,
                errorMessage = null,
                onScan = {},
                onJoin = {})
        }
    }
}

@Preview(name = "Error", showBackground = true)
@Composable
private fun JoinTeamSheetErrorPreview() {
    WandrTheme {
        Surface {
            JoinTeamSheetContent(
                "NOPE",
                {},
                isJoining = false,
                errorMessage = "Invalid invite code",
                onScan = {},
                onJoin = {})
        }
    }
}
