package com.wandr.android.ui.team

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.LabeledValue
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.common.CoverEditor
import com.wandr.android.ui.common.AvatarEditor
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamMember
import com.wandr.domain.model.TeamRole
import com.wandr.presentation.teamdetails.TeamDetailsIntent
import com.wandr.presentation.teamdetails.TeamDetailsState
import com.wandr.presentation.teamdetails.TeamDetailsSuccess
import com.wandr.presentation.teamdetails.TeamDetailsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun TeamDetailsScreen(
    teamId: String,
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TeamDetailsViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(teamId, userId) { viewModel.processIntent(TeamDetailsIntent.Load(teamId, userId)) }
    TeamDetailsScreenContent(state = state, onIntent = viewModel::processIntent, onBack = onBack, modifier = modifier)
}

/**
 * Read-only group details with the member list. Owners and admins get an "Edit" button that opens
 * [TeamEditScreen] in a bottom sheet; saving closes the sheet. Results (saved / image updated / error) are
 * reported in a snackbar (on the sheet while it is open).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamDetailsScreenContent(
    state: TeamDetailsState,
    onIntent: (TeamDetailsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    // Only a save started from the sheet may close it (not, e.g., an image update).
    var saveRequested by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val team = state.team
    val isSaving by rememberUpdatedState(state.isSaving)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !isSaving } // no swipe-away while saving
    )

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { isEditing = false }
    }

    val savedText = stringResource(R.string.team_saved_message)
    val imageText = stringResource(R.string.team_image_updated_message)
    LaunchedEffect(state.success, state.errorMessage) {
        val message = when {
            state.errorMessage != null -> state.errorMessage
            state.success == TeamDetailsSuccess.TEAM_SAVED -> savedText
            state.success == TeamDetailsSuccess.IMAGE_UPDATED -> imageText
            else -> null
        } ?: return@LaunchedEffect

        if (state.success == TeamDetailsSuccess.TEAM_SAVED && saveRequested) closeSheet()
        saveRequested = false
        // Own scope: clearing the message changes the effect keys, which would cancel (and hide) the snackbar.
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
        onIntent(TeamDetailsIntent.ClearMessages)
    }
    // Losing edit rights (e.g. role changed remotely) leaves edit mode.
    LaunchedEffect(state.canEdit) { if (!state.canEdit) isEditing = false }

    ScreenScaffold(
        title = stringResource(R.string.team_details_title),
        modifier = modifier,
        onBack = onBack,
        actions = {
            if (state.canEdit && team != null) {
                TextButton(onClick = { isEditing = true }) { Text(stringResource(R.string.profile_edit_button)) }
            }
        },
        // While the sheet is open, its own host shows the messages.
        snackbarHost = { if (!isEditing) SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading || team == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Owners and admins change cover and avatar right here (tap), no edit mode needed.
                    CoverEditor(
                        coverUrl = team.coverUrl,
                        isBusy = state.isImageUpdating,
                        enabled = state.canEdit,
                        onCoverReady = { onIntent(TeamDetailsIntent.UploadCover(it)) },
                        onRemoveCover = { onIntent(TeamDetailsIntent.RemoveCover) }
                    )

                    Spacer(Modifier.height(16.dp))

                    AvatarEditor(
                        avatarUrl = team.avatarUrl,
                        displayName = team.name,
                        isBusy = state.isImageUpdating,
                        enabled = state.canEdit,
                        title = stringResource(R.string.team_photo_title),
                        onAvatarReady = { onIntent(TeamDetailsIntent.UploadAvatar(it)) },
                        onRemoveAvatar = { onIntent(TeamDetailsIntent.RemoveAvatar) }
                    )

                    Spacer(Modifier.height(8.dp))

                    LabeledValue(stringResource(R.string.team_name_label), team.name)
                    LabeledValue(stringResource(R.string.team_description_label), team.description)

                    Text(
                        text = stringResource(R.string.group_invite_code, team.inviteCode),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.team_members_count, state.members.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.members.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.team_no_members),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        state.members.forEach { member -> TeamMemberRow(member) }
                    }
                }
            }
        }
    }

    if (isEditing && team != null) {
        ModalBottomSheet(
            onDismissRequest = {
                onIntent(TeamDetailsIntent.DiscardChanges)
                isEditing = false
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            TeamEditScreen(
                title = stringResource(R.string.team_edit_title),
                name = team.name,
                description = team.description.orEmpty(),
                onNameChange = { onIntent(TeamDetailsIntent.NameChanged(it)) },
                onDescriptionChange = { onIntent(TeamDetailsIntent.DescriptionChanged(it)) },
                isSaving = state.isSaving,
                onSave = {
                    saveRequested = true
                    onIntent(TeamDetailsIntent.Save)
                },
                onCancel = {
                    onIntent(TeamDetailsIntent.DiscardChanges)
                    closeSheet()
                },
                snackbarHostState = snackbarHostState
            )
        }
    }
}

private val previewTeam = Team(
    id = "t1", name = "Alpine Trail Blazers", description = "Hiking group for weekend trips.",
    avatarUrl = null, coverUrl = null, inviteCode = "X7K9P2W1", createdBy = "u1", createdAt = 0L, updatedAt = 0L
)

private val previewMembers = listOf(
    TeamMember("1", "t1", "u1", TeamRole.ADMIN, "martinkade", "Martin Kade", null, 0L),
    TeamMember("2", "t1", "u2", TeamRole.MEMBER, "runner99", "Alex Runner", null, 0L),
    TeamMember("3", "t1", "u3", TeamRole.MEMBER, "sophie", "Sophie Hiker", null, 0L)
)

@Preview(name = "Read-only", showBackground = true)
@Preview(name = "Read-only Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun TeamDetailsScreenPreview() {
    WandrTheme {
        TeamDetailsScreenContent(
            state = TeamDetailsState(team = previewTeam, members = previewMembers, canEdit = true),
            onIntent = {}, onBack = {}
        )
    }
}
