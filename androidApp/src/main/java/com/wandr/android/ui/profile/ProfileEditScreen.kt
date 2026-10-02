package com.wandr.android.ui.profile

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
import com.wandr.domain.model.Profile
import com.wandr.presentation.profile.ProfileIntent
import com.wandr.presentation.profile.ProfileState

/** Edit screen shown in the profile's bottom sheet: photo, name, bio and Cancel / Save. */
@Composable
fun ProfileEditScreen(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
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
        Text(
            text = stringResource(R.string.profile_edit_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = state.profile?.displayName ?: "",
            onValueChange = { onIntent(ProfileIntent.DisplayNameChanged(it)) },
            label = { Text(stringResource(R.string.display_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.profile?.bio ?: "",
            onValueChange = { onIntent(ProfileIntent.BioChanged(it)) },
            label = { Text(stringResource(R.string.bio_label)) },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, enabled = !state.isSaving, modifier = Modifier.weight(1f).height(50.dp)) {
                Text(stringResource(R.string.cancel_button))
            }
            Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.weight(1f).height(50.dp)) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.save_profile_button))
                }
            }
        }

        // Messages while the sheet is open (e.g. photo updated, errors) are shown here, above the sheet.
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(vertical = 16.dp))
    }
}

private val previewProfile = Profile(
    id = "1", username = "martinkade", displayName = "Martin Kade", avatarUrl = null,
    bio = "Outdoor hiker & developer.", createdAt = 0L, updatedAt = 0L
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Preview(name = "Tablet", widthDp = 840, heightDp = 900, showBackground = true)
@Composable
private fun ProfileEditScreenPreview() {
    WandrTheme {
        ProfileEditScreen(
            state = ProfileState(profile = previewProfile),
            onIntent = {}, onSave = {}, onCancel = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
