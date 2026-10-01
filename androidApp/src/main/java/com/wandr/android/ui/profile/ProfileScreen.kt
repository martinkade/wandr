package com.wandr.android.ui.profile

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.R
import com.wandr.domain.model.Profile
import com.wandr.presentation.profile.ProfileIntent
import com.wandr.presentation.profile.ProfileState

@Composable
fun ProfileScreen(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.user_profile_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )

                Spacer(modifier = Modifier.height(24.dp))

                AvatarEditor(
                    avatarUrl = state.profile?.avatarUrl,
                    displayName = state.profile?.displayName ?: "User",
                    isBusy = state.isAvatarUpdating || state.profile == null,
                    onAvatarReady = { jpeg ->
                        state.profile?.let { onIntent(ProfileIntent.UploadAvatar(it.id, jpeg)) }
                    },
                    onRemoveAvatar = {
                        state.profile?.let { onIntent(ProfileIntent.RemoveAvatar(it.id)) }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = state.profile?.displayName ?: "",
                    onValueChange = { onIntent(ProfileIntent.DisplayNameChanged(it)) },
                    label = { Text(stringResource(R.string.display_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.profile?.bio ?: "",
                    onValueChange = { onIntent(ProfileIntent.BioChanged(it)) },
                    label = { Text(stringResource(R.string.bio_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )

                state.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                state.successMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (state.isSaving) {
                    CircularProgressIndicator()
                } else {
                    Button(
                        onClick = { onIntent(ProfileIntent.SaveProfile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(stringResource(R.string.save_profile_button))
                    }
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    WandrTheme {
        ProfileScreen(
            state = ProfileState(
                profile = Profile(
                    id = "1",
                    username = "martinkade",
                    displayName = "Martin Kade",
                    avatarUrl = null,
                    bio = "Outdoor hiker & developer.",
                    createdAt = 0L,
                    updatedAt = 0L
                )
            ),
            onIntent = {}
        )
    }
}
