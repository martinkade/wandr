package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.common.userMessage
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeParticipation
import com.wandr.domain.model.ChallengeStatus
import com.wandr.domain.model.isVisibleTo
import com.wandr.presentation.challenge.ChallengeIntent
import com.wandr.presentation.challenge.ChallengeState
import com.wandr.presentation.challenge.ChallengeSuccess
import com.wandr.presentation.challenge.ChallengeViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * All challenges the user can see: open ones (for everyone), their own and those of their groups.
 * Managers can create challenges and edit their own via a bottom sheet.
 */
@Composable
fun ChallengeListScreen(
    userId: String,
    isManager: Boolean,
    onOpenChallenge: (challengeId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChallengeViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(userId) { viewModel.processIntent(ChallengeIntent.LoadChallenges(userId)) }
    ChallengeListScreenContent(
        state = state,
        userId = userId,
        isManager = isManager,
        onIntent = viewModel::processIntent,
        onOpenChallenge = onOpenChallenge,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChallengeListScreenContent(
    state: ChallengeState,
    userId: String,
    isManager: Boolean,
    onIntent: (ChallengeIntent) -> Unit,
    onOpenChallenge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Drafts are for their creating manager only (the server filters too; the local cache may be older).
    val visibleChallenges = remember(state.challenges, userId, isManager) {
        state.challenges.filter { it.isVisibleTo(userId, isManager) }
    }
    var isEditing by rememberSaveable { mutableStateOf(false) }
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

    val createdText = stringResource(R.string.challenge_created_message)
    val updatedText = stringResource(R.string.challenge_updated_message)
    val joinedText = stringResource(R.string.challenge_joined_message)
    val enrolledText = stringResource(R.string.challenge_team_enrolled_message)
    val leftText = stringResource(R.string.challenge_left_message)
    val withdrawnText = stringResource(R.string.challenge_team_withdrawn_message)
    val errorText = state.error?.userMessage()
    LaunchedEffect(state.success, state.error) {
        val message = when {
            errorText != null -> errorText
            state.success == ChallengeSuccess.CREATED -> createdText
            state.success == ChallengeSuccess.UPDATED -> updatedText
            state.success == ChallengeSuccess.JOINED -> joinedText
            state.success == ChallengeSuccess.TEAM_ENROLLED -> enrolledText
            state.success == ChallengeSuccess.LEFT -> leftText
            state.success == ChallengeSuccess.TEAM_WITHDRAWN -> withdrawnText
            else -> null
        } ?: return@LaunchedEffect

        val saved = state.success == ChallengeSuccess.CREATED || state.success == ChallengeSuccess.UPDATED
        if (saved && saveRequested) closeSheet()
        saveRequested = false
        // Own scope: clearing the message changes the effect keys, which would cancel (and hide) the snackbar.
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
        onIntent(ChallengeIntent.ClearMessages)
    }

    ScreenScaffold(
        title = stringResource(R.string.challenges_title),
        modifier = modifier,
        // While the sheet is open, its own host shows the messages.
        snackbarHost = { if (!isEditing) SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (isManager) {
                ExtendedFloatingActionButton(onClick = {
                    onIntent(ChallengeIntent.StartCreate)
                    isEditing = true
                }) { Text(stringResource(R.string.challenge_new_button)) }
            }
        }
    ) { padding ->
        Box(Modifier
            .fillMaxSize()
            .padding(padding)) {
            if (visibleChallenges.isEmpty() && !state.isLoading) {
                Text(
                    text = stringResource(R.string.challenges_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Bottom padding keeps the last card clear of the floating button.
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(visibleChallenges, key = { it.id }) { challenge ->
                        val status = state.statuses[challenge.id] ?: ChallengeStatus.ACTIVE
                        // Joining, leaving and editing happen on the details screen.
                        ChallengeCard(
                            challenge = challenge,
                            status = status,
                            onClick = { onOpenChallenge(challenge.id) }
                        )
                    }
                }
            }
        }
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

private val previewChallenges = listOf(
    Challenge("c1", "30-Day 100 km Hike", "Open to everyone.", null, "individual", "distance", 100000.0, false, 1_768_435_200_000L, 1_771_027_200_000L, "u1", 0L, 0L),
    Challenge("c2", "5000 m Altitude Climb", "Teams compete.", null, "group", "elevation", 5000.0, true, 1_768_435_200_000L, 1_769_039_940_000L, "u2", 0L, 0L)
)

@Preview(name = "Manager", showBackground = true)
@Preview(name = "Manager Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Tablet", widthDp = 840, heightDp = 700, showBackground = true)
@Composable
private fun ChallengeListScreenManagerPreview() {
    WandrTheme {
        ChallengeListScreenContent(
            state = ChallengeState(
                challenges = previewChallenges,
                teams = listOf(com.wandr.domain.model.Team("t1", "Alpine Trail Blazers", null, null, null, "X7K9P2W1", "u1", 0L, 0L))
            ),
            userId = "u1", isManager = true, onIntent = {}, onOpenChallenge = {}
        )
    }
}

@Preview(name = "Joined (leave button)", showBackground = true)
@Composable
private fun ChallengeListScreenJoinedPreview() {
    WandrTheme {
        ChallengeListScreenContent(
            state = ChallengeState(
                challenges = previewChallenges,
                statuses = mapOf("c1" to ChallengeStatus.ACTIVE, "c2" to ChallengeStatus.EXPIRED),
                participations = mapOf("c1" to ChallengeParticipation("c1", null))
            ),
            userId = "u3", isManager = false, onIntent = {}, onOpenChallenge = {}
        )
    }
}

@Preview(name = "Member", showBackground = true)
@Composable
private fun ChallengeListScreenMemberPreview() {
    WandrTheme {
        ChallengeListScreenContent(
            state = ChallengeState(challenges = previewChallenges), userId = "u3", isManager = false, onIntent = {}, onOpenChallenge = {}
        )
    }
}

@Preview(name = "Empty", showBackground = true)
@Preview(name = "Empty Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ChallengeListScreenEmptyPreview() {
    WandrTheme {
        ChallengeListScreenContent(state = ChallengeState(), userId = "u1", isManager = true, onIntent = {}, onOpenChallenge = {})
    }
}
