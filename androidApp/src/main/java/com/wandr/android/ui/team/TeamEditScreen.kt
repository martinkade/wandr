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
import com.wandr.android.ui.common.rememberImagePickerFlow
import com.wandr.android.ui.profile.AvatarEditor
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Team
import com.wandr.presentation.imagecrop.CoverImageSpec
import com.wandr.presentation.teamdetails.TeamDetailsIntent
import com.wandr.presentation.teamdetails.TeamDetailsState

/** Edit screen shown in the group's bottom sheet: cover (4:3), avatar (1:1), name, description, Cancel / Save. */
@Composable
fun TeamEditScreen(
    state: TeamDetailsState,
    onIntent: (TeamDetailsIntent) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val team = state.team
    val coverFlow = rememberImagePickerFlow(
        title = stringResource(R.string.team_cover_title),
        aspectRatio = CoverImageSpec.ASPECT_RATIO,
        outputMaxEdgePx = CoverImageSpec.MAX_EDGE_PX,
        jpegQuality = CoverImageSpec.JPEG_QUALITY,
        canRemove = team?.coverUrl != null,
        onImageReady = { onIntent(TeamDetailsIntent.UploadCover(it)) },
        onRemove = { onIntent(TeamDetailsIntent.RemoveCover) }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.team_edit_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.height(20.dp))

        TeamCover(coverUrl = team?.coverUrl, isEditing = true, isBusy = state.isImageUpdating, onChange = coverFlow::open)
        Spacer(Modifier.height(16.dp))

        AvatarEditor(
            avatarUrl = team?.avatarUrl,
            displayName = team?.name.orEmpty(),
            isBusy = state.isImageUpdating,
            title = stringResource(R.string.team_photo_title),
            onAvatarReady = { onIntent(TeamDetailsIntent.UploadAvatar(it)) },
            onRemoveAvatar = { onIntent(TeamDetailsIntent.RemoveAvatar) }
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = team?.name.orEmpty(),
            onValueChange = { onIntent(TeamDetailsIntent.NameChanged(it)) },
            label = { Text(stringResource(R.string.team_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = team?.description.orEmpty(),
            onValueChange = { onIntent(TeamDetailsIntent.DescriptionChanged(it)) },
            label = { Text(stringResource(R.string.team_description_label)) },
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

        // Messages while the sheet is open (e.g. image updated, errors) are shown here, above the sheet.
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(vertical = 16.dp))
    }
}

private val previewTeam = Team(
    id = "t1", name = "Alpine Trail Blazers", description = "Hiking group for weekend trips.",
    avatarUrl = null, coverUrl = null, inviteCode = "X7K9P2W1", createdBy = "u1", createdAt = 0L, updatedAt = 0L
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Preview(name = "Tablet", widthDp = 840, heightDp = 1000, showBackground = true)
@Composable
private fun TeamEditScreenPreview() {
    WandrTheme {
        TeamEditScreen(
            state = TeamDetailsState(team = previewTeam, canEdit = true),
            onIntent = {}, onSave = {}, onCancel = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
