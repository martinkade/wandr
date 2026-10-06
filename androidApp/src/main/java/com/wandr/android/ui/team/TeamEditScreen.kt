package com.wandr.android.ui.team

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/**
 * Name and description form shown in a bottom sheet, for creating a group and for editing one. Photos are not part
 * of the form: avatar and cover are changed directly on the group's detail screen.
 */
@Composable
fun TeamEditScreen(
    title: String,
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.team_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.team_description_label)) },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.weight(1f).height(50.dp)) {
                Text(stringResource(R.string.cancel_button))
            }
            Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.weight(1f).height(50.dp)) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.save_team_button))
                }
            }
        }

        // Messages while the sheet is open (errors) are shown here, above the sheet.
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(vertical = 16.dp))
    }
}

@Preview(name = "Create", showBackground = true)
@Preview(name = "Create Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun TeamEditScreenCreatePreview() {
    WandrTheme {
        TeamEditScreen(
            title = stringResource(R.string.create_team_title), name = "", description = "",
            onNameChange = {}, onDescriptionChange = {}, isSaving = false, onSave = {}, onCancel = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(name = "Edit", showBackground = true)
@Preview(name = "Edit Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Tablet", widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun TeamEditScreenEditPreview() {
    WandrTheme {
        TeamEditScreen(
            title = stringResource(R.string.team_edit_title), name = "Alpine Trail Blazers",
            description = "Hiking group for weekend trips.",
            onNameChange = {}, onDescriptionChange = {}, isSaving = true, onSave = {}, onCancel = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
