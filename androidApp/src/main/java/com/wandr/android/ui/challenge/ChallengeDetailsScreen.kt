package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.wandr.android.ui.common.CoverImage
import com.wandr.android.ui.common.LabeledValue
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.common.rememberImagePickerFlow
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeScope
import com.wandr.domain.model.ChallengeStatus
import com.wandr.domain.model.ChallengeType
import com.wandr.domain.model.availableChallengeAction
import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamStanding
import com.wandr.presentation.challenge.ChallengeIntent
import com.wandr.presentation.challenge.ChallengeState
import com.wandr.presentation.challenge.ChallengeSuccess
import com.wandr.presentation.challenge.ChallengeViewModel
import com.wandr.presentation.imagecrop.CoverImageSpec
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Read-only challenge details. The manager who created the challenge can change the cover directly here (tap it)
 * and edit the other fields via the "Edit" button, which opens a bottom sheet. Group challenges show the
 * team-vs-team standings and let a team owner / admin enroll a team.
 */
@Composable
fun ChallengeDetailsScreen(
    challengeId: String,
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChallengeViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(challengeId, userId) {
        viewModel.processIntent(ChallengeIntent.LoadChallenges(userId)) // teams for enrolling, runtime statuses
        viewModel.processIntent(ChallengeIntent.SelectChallenge(challengeId))
    }
    ChallengeDetailsScreenContent(
        state = state,
        userId = userId,
        onBack = onBack,
        onIntent = viewModel::processIntent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChallengeDetailsScreenContent(
    state: ChallengeState,
    userId: String,
    onBack: () -> Unit,
    onIntent: (ChallengeIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val challenge = state.selectedChallenge
    // Only the creator (owner) may edit the challenge or change its cover; the server enforces the same rule.
    val canEdit = state.canEdit

    var isEditing by rememberSaveable { mutableStateOf(false) }
    var enrolling by rememberSaveable { mutableStateOf(false) }
    // Only a save started from the sheet may close it.
    var saveRequested by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isSaving by rememberUpdatedState(state.isSaving)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !isSaving } // no swipe-away while saving
    )

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            isEditing = false
            onIntent(ChallengeIntent.DiscardForm)
        }
    }

    val updatedText = stringResource(R.string.challenge_updated_message)
    val joinedText = stringResource(R.string.challenge_joined_message)
    val enrolledText = stringResource(R.string.challenge_team_enrolled_message)
    val leftText = stringResource(R.string.challenge_left_message)
    val withdrawnText = stringResource(R.string.challenge_team_withdrawn_message)
    val imageText = stringResource(R.string.challenge_image_updated_message)
    LaunchedEffect(state.success, state.errorMessage) {
        val message = when {
            state.errorMessage != null -> state.errorMessage
            state.success == ChallengeSuccess.UPDATED -> updatedText
            state.success == ChallengeSuccess.JOINED -> joinedText
            state.success == ChallengeSuccess.TEAM_ENROLLED -> enrolledText
            state.success == ChallengeSuccess.LEFT -> leftText
            state.success == ChallengeSuccess.TEAM_WITHDRAWN -> withdrawnText
            state.success == ChallengeSuccess.IMAGE_UPDATED -> imageText
            else -> null
        } ?: return@LaunchedEffect

        if (state.success == ChallengeSuccess.UPDATED && saveRequested) closeSheet()
        saveRequested = false
        // Own scope: clearing the message changes the effect keys, which would cancel (and hide) the snackbar.
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
        onIntent(ChallengeIntent.ClearMessages)
    }

    val coverFlow = rememberImagePickerFlow(
        title = stringResource(R.string.team_cover_title),
        aspectRatio = CoverImageSpec.ASPECT_RATIO,
        outputMaxEdgePx = CoverImageSpec.MAX_EDGE_PX,
        jpegQuality = CoverImageSpec.JPEG_QUALITY,
        canRemove = challenge?.coverUrl != null,
        onImageReady = { onIntent(ChallengeIntent.UploadCover(it)) },
        onRemove = { onIntent(ChallengeIntent.RemoveCover) }
    )

    ScreenScaffold(
        title = stringResource(R.string.challenge_details_title),
        modifier = modifier,
        onBack = onBack,
        actions = {
            if (canEdit && challenge != null) {
                TextButton(onClick = {
                    onIntent(ChallengeIntent.StartEdit(challenge.id))
                    isEditing = true
                }) { Text(stringResource(R.string.challenge_edit_button)) }
            }
        },
        // While the sheet is open, its own host shows the messages.
        snackbarHost = { if (!isEditing) SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (challenge == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val type = ChallengeType.fromValue(challenge.type)
                val isGroup = ChallengeScope.fromValue(challenge.scope) == ChallengeScope.GROUP
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CoverImage(
                        coverUrl = challenge.coverUrl,
                        isEditable = canEdit,
                        isBusy = state.isImageUpdating,
                        onClick = coverFlow::open
                    )
                    Spacer(Modifier.height(8.dp))

                    LabeledValue(stringResource(R.string.challenge_title_label), challenge.title)
                    LabeledValue(stringResource(R.string.challenge_description_label), challenge.description)
                    LabeledValue(
                        stringResource(R.string.challenge_status_label),
                        stringResource(statusLabel(state.statuses[challenge.id] ?: ChallengeStatus.ACTIVE))
                    )
                    LabeledValue(
                        stringResource(R.string.challenge_target_label),
                        "${stringResource(typeLabel(type))}: ${challengeValueText(type, challenge.targetValue)}"
                    )
                    LabeledValue(stringResource(R.string.challenge_period_label), challengePeriodText(challenge))
                    LabeledValue(
                        stringResource(R.string.challenge_scope_label),
                        stringResource(if (isGroup) R.string.challenge_scope_group else R.string.challenge_scope_individual)
                    )
                    if (isGroup && challenge.requireAllMembersCompletion) {
                        Text(
                            text = stringResource(R.string.require_all_members_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    // Join / leave (individual) or enroll / withdraw a team (group); none for drafts and finished challenges.
                    val status = state.statuses[challenge.id] ?: ChallengeStatus.ACTIVE
                    val participation = state.participations[challenge.id]
                    val action = availableChallengeAction(challenge, status, participation, state.teams.isNotEmpty())
                    action?.let {
                        ChallengeActionButton(
                            action = it,
                            onClick = {
                                performChallengeAction(it, challenge.id, participation, userId, onIntent) { enrolling = true }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (isGroup) {
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.challenge_standings_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.standings.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.challenge_standings_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            state.standings.forEach { standing -> TeamStandingRow(standing, type) }
                        }
                    }
                }
            }
        }
    }

    if (enrolling) {
        EnrollTeamDialog(
            teams = state.teams,
            onTeamSelected = { team ->
                enrolling = false
                challenge?.let { onIntent(ChallengeIntent.EnrollTeam(it.id, team.id, userId)) }
            },
            onDismiss = { enrolling = false }
        )
    }

    val form = state.form
    if (isEditing && form != null) {
        ModalBottomSheet(
            onDismissRequest = {
                onIntent(ChallengeIntent.DiscardForm)
                isEditing = false
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            ChallengeEditScreen(
                form = form,
                isSaving = state.isSaving,
                onIntent = onIntent,
                onSave = {
                    saveRequested = true
                    onIntent(ChallengeIntent.SubmitForm(userId))
                },
                onCancel = ::closeSheet,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

private fun typeLabel(type: ChallengeType) = when (type) {
    ChallengeType.DISTANCE -> R.string.challenge_type_distance
    ChallengeType.ELEVATION -> R.string.challenge_type_elevation
    ChallengeType.TIME -> R.string.challenge_type_time
}

private fun statusLabel(status: ChallengeStatus) = when (status) {
    ChallengeStatus.DRAFT -> R.string.challenge_status_draft
    ChallengeStatus.PLANNED -> R.string.challenge_status_planned
    ChallengeStatus.ACTIVE -> R.string.challenge_status_active
    ChallengeStatus.COMPLETED -> R.string.challenge_status_completed
    ChallengeStatus.EXPIRED -> R.string.challenge_status_expired
}

private val previewGroupChallenge = Challenge(
    id = "c2", title = "Team Altitude Climb", description = "Teams compete for 5000 m.", coverUrl = null,
    scope = "group", type = "elevation", targetValue = 5000.0, requireAllMembersCompletion = true,
    startDate = 1_768_435_200_000L, endDate = 1_771_027_200_000L, createdBy = "u1", createdAt = 0L, updatedAt = 0L
)

@Preview(name = "Group, creator", showBackground = true, heightDp = 1100)
@Preview(name = "Group Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, heightDp = 1100)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 1400)
@Composable
private fun ChallengeDetailsScreenGroupPreview() {
    WandrTheme {
        ChallengeDetailsScreenContent(
            state = ChallengeState(
                selectedChallenge = previewGroupChallenge,
                canEdit = true, // the creator sees "Edit" and can change the cover
                statuses = mapOf("c2" to ChallengeStatus.ACTIVE),
                teams = listOf(Team("t1", "Alpine Trail Blazers", null, null, null, "X7K9P2W1", "u1", 0L, 0L)),
                standings = listOf(
                    TeamStanding(1, "t2", "City Runners", null, 3_800.0, 76.0, 4, 1, false),
                    TeamStanding(2, "t1", "Alpine Trail Blazers", null, 2_500.0, 50.0, 3, 0, false)
                )
            ),
            userId = "u1", onBack = {}, onIntent = {}
        )
    }
}

@Preview(name = "Individual draft, not the creator", showBackground = true, heightDp = 900)
@Composable
private fun ChallengeDetailsScreenIndividualPreview() {
    WandrTheme {
        ChallengeDetailsScreenContent(
            state = ChallengeState(
                selectedChallenge = previewGroupChallenge.copy(scope = "individual", isActive = false),
                statuses = mapOf("c2" to ChallengeStatus.DRAFT)
            ),
            userId = "u3", onBack = {}, onIntent = {}
        )
    }
}
