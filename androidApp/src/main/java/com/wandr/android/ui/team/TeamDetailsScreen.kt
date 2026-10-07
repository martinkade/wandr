package com.wandr.android.ui.team

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.AvatarImagePicker
import com.wandr.android.ui.common.CollapsingHeaderScaffold
import com.wandr.android.ui.common.CoverHero
import com.wandr.android.ui.common.CoverHeroHeight
import com.wandr.android.ui.common.OverflowMenu
import com.wandr.android.ui.common.OverflowMenuItem
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.common.SheetOverlap
import com.wandr.android.ui.common.rememberImagePickerFlow
import com.wandr.android.ui.common.userMessage
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamMember
import com.wandr.domain.model.TeamRole
import com.wandr.presentation.imagecrop.AvatarImageSpec
import com.wandr.presentation.imagecrop.CoverImageSpec
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
    LaunchedEffect(teamId, userId) {
        viewModel.processIntent(
            TeamDetailsIntent.Load(
                teamId,
                userId
            )
        )
    }
    TeamDetailsScreenContent(
        state = state,
        onIntent = viewModel::processIntent,
        onBack = onBack,
        modifier = modifier
    )
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
    var showInvite by rememberSaveable { mutableStateOf(false) }
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
    val errorText = state.error?.userMessage()
    LaunchedEffect(state.success, state.error) {
        val message = when {
            errorText != null -> errorText
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

    // While the sheet is open, its own host shows the messages.
    val snackbarHost: @Composable () -> Unit = { if (!isEditing) SnackbarHost(snackbarHostState) }

    if (state.isLoading || team == null) {
        ScreenScaffold(
            title = stringResource(R.string.team_details_title),
            modifier = modifier,
            onBack = onBack,
            snackbarHost = snackbarHost
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
    } else {
        // Owners and admins change cover and avatar right here (tap), no edit mode needed.
        val coverFlow = rememberImagePickerFlow(
            title = stringResource(R.string.team_cover_title),
            aspectRatio = CoverImageSpec.ASPECT_RATIO,
            outputMaxEdgePx = CoverImageSpec.MAX_EDGE_PX,
            jpegQuality = CoverImageSpec.JPEG_QUALITY,
            canRemove = team.coverUrl != null,
            onImageReady = { onIntent(TeamDetailsIntent.UploadCover(it)) },
            onRemove = { onIntent(TeamDetailsIntent.RemoveCover) }
        )

        val avatarFlow = rememberImagePickerFlow(
            title = stringResource(R.string.team_photo_title),
            aspectRatio = AvatarImageSpec.ASPECT_RATIO,
            outputMaxEdgePx = AvatarImageSpec.MAX_EDGE_PX,
            jpegQuality = AvatarImageSpec.JPEG_QUALITY,
            canRemove = team.avatarUrl != null,
            onImageReady = { onIntent(TeamDetailsIntent.UploadAvatar(it)) },
            onRemove = { onIntent(TeamDetailsIntent.RemoveAvatar) }
        )

        // The cover scrolls away with a parallax effect; the top bar turns solid and shows the group's name.
        CollapsingHeaderScaffold(
            title = team.name,
            onBack = onBack,
            headerHeight = CoverHeroHeight,
            modifier = modifier,
            header = {
                CoverHero(
                    coverUrl = team.coverUrl,
                    seed = team.id.hashCode(),
                    isBusy = state.isImageUpdating,
                )
            },
            actions = { contentColor ->
                if (state.canEdit) {
                    OverflowMenu(
                        contentColor = contentColor,
                        items = listOf(
                            OverflowMenuItem(
                                label = stringResource(R.string.profile_edit_button),
                                icon = R.drawable.ic_edit,
                                onClick = { isEditing = true }
                            ),
                            OverflowMenuItem(
                                label = stringResource(R.string.challenge_change_cover),
                                icon = R.drawable.ic_image,
                                onClick = coverFlow::open
                            ),
                            OverflowMenuItem(
                                label = stringResource(R.string.team_change_photo),
                                icon = R.drawable.ic_image,
                                onClick = avatarFlow::open
                            )
                        )
                    )
                }
            },
            snackbarHost = snackbarHost
        ) {
            Box(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .offset(y = -SheetOverlap)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = SheetOverlap, topEnd = SheetOverlap))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            top = AvatarSize / 2 + 12.dp,
                            bottom = 32.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    team.description?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // The invite sheet shows a QR code that others scan to join the group.
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(onClick = { showInvite = true }) {
                        Text(stringResource(R.string.team_invite_button))
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = pluralStringResource(
                            R.plurals.team_members_count,
                            state.members.size,
                            state.members.size
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
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
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            state.members.forEachIndexed { index, member ->
                                TeamMemberRow(
                                    member,
                                    isFirstItemInSection = index == 0,
                                    isLastItemInSection = index == state.members.lastIndex
                                )
                            }
                        }
                    }
                }

                // The avatar sits on the edge between cover and sheet.
                AvatarImagePicker(
                    avatarUrl = team.avatarUrl,
                    displayName = team.name,
                    onPickAvatar = avatarFlow::open,
                    isBusy = state.isImageUpdating,
                    enabled = state.canEdit,
                    size = AvatarSize,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = -SheetOverlap - AvatarSize / 2)
                )
            }
        }
    }

    if (showInvite && team != null) {
        InviteSheet(
            teamName = team.name,
            inviteCode = team.inviteCode,
            onDismiss = { showInvite = false })
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

/** The size of the avatar on the edge between cover and sheet. */
private val AvatarSize = 96.dp

private val previewTeam = Team(
    id = "t1",
    name = "Alpine Trail Blazers",
    description = "Hiking group for weekend trips.",
    avatarUrl = null,
    coverUrl = null,
    inviteCode = "X7K9P2W1",
    createdBy = "u1",
    createdAt = 0L,
    updatedAt = 0L
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
