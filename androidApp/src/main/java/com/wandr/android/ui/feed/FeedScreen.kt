package com.wandr.android.ui.feed

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.wandr.android.ui.activity.ActivityCard
import com.wandr.android.ui.activity.ActivityConflictSheet
import com.wandr.android.ui.activity.ActivityEditScreen
import com.wandr.android.ui.activity.ActivityFabMenu
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Activity
import com.wandr.presentation.activity.ActivityIntent
import com.wandr.presentation.activity.ActivityState
import com.wandr.presentation.activity.ActivitySuccess
import com.wandr.presentation.activity.ActivityViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * The feed: the user's activities, and the team's. A FAB menu offers to record an activity ([onRecord]) or to log one manually, which opens a
 * bottom sheet. Opening an activity ([onOpenActivity]) shows its details, where the owner can edit it.
 *
 * @param actions extra top bar buttons (e.g. the notifications bell)
 * @param teamId the team new manual activities count for, if the user has one
 */
@Composable
fun FeedScreen(
    userId: String,
    teamId: String?,
    onOpenActivity: (activityId: String) -> Unit,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    viewModel: ActivityViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    var feed by rememberSaveable { mutableStateOf(FeedScope.Mine) }
    // The team feed shows what all members of the team recorded.
    LaunchedEffect(userId, teamId, feed) {
        if (feed == FeedScope.Team && teamId != null) {
            viewModel.processIntent(ActivityIntent.LoadTeamActivities(teamId))
        } else {
            viewModel.processIntent(ActivityIntent.LoadUserActivities(userId))
        }
    }
    FeedScreenContent(
        state = state,
        userId = userId,
        teamId = teamId,
        onIntent = viewModel::processIntent,
        onOpenActivity = onOpenActivity,
        onRecord = onRecord,
        modifier = modifier,
        feed = feed,
        onFeedChange = { feed = it },
        actions = actions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedScreenContent(
    state: ActivityState,
    userId: String,
    teamId: String?,
    onIntent: (ActivityIntent) -> Unit,
    onOpenActivity: (String) -> Unit,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
    feed: FeedScope = FeedScope.Mine,
    onFeedChange: (FeedScope) -> Unit = {},
    actions: @Composable () -> Unit = {}
) {
    var isCreating by rememberSaveable { mutableStateOf(false) }
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
            isCreating = false
            onIntent(ActivityIntent.DiscardForm)
        }
    }

    val createdText = stringResource(R.string.activity_created_message)
    val deletedText = stringResource(R.string.activity_deleted_message)
    val mergedText = stringResource(R.string.activity_merged_message)
    val trimmedText = stringResource(R.string.activity_trimmed_message)
    val discardedText = stringResource(R.string.activity_discarded_message)
    LaunchedEffect(state.success, state.errorMessage) {
        val message = when {
            state.errorMessage != null -> state.errorMessage
            state.success == ActivitySuccess.CREATED -> createdText
            state.success == ActivitySuccess.DELETED -> deletedText
            state.success == ActivitySuccess.MERGED -> mergedText
            state.success == ActivitySuccess.TRIMMED -> trimmedText
            state.success == ActivitySuccess.DISCARDED -> discardedText
            else -> null
        } ?: return@LaunchedEffect

        if (state.success != null && state.success != ActivitySuccess.DELETED && saveRequested) closeSheet()
        saveRequested = false
        // Own scope: clearing the message changes the effect keys, which would cancel (and hide) the snackbar.
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
        onIntent(ActivityIntent.ClearMessages)
    }

    ScreenScaffold(
        title = stringResource(R.string.feed_title),
        modifier = modifier,
        actions = { actions() },
        // While the sheet is open, its own host shows the messages.
        snackbarHost = { if (!isCreating) SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ActivityFabMenu(
                onCreate = {
                    onIntent(ActivityIntent.StartCreate)
                    isCreating = true
                },
                onRecord = onRecord
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (teamId != null) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    FeedScope.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = feed == option,
                            onClick = { onFeedChange(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, FeedScope.entries.size)
                        ) { Text(stringResource(option.labelRes)) }
                    }
                }
            }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.activities.isEmpty()) {
                Text(
                    text = stringResource(R.string.activities_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    // Bottom padding keeps the last card clear of the floating button.
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
                ) {
                    items(state.activities, key = { it.id }) { activity ->
                        ActivityCard(activity = activity, onClick = { onOpenActivity(activity.id) })
                    }
                }
            }
        }
        }
    }

    val form = state.form
    if (isCreating && form != null) {
        ModalBottomSheet(
            onDismissRequest = {
                onIntent(ActivityIntent.DiscardForm)
                isCreating = false
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            ActivityEditScreen(
                form = form,
                isSaving = state.isSaving,
                onIntent = onIntent,
                onSave = {
                    saveRequested = true
                    onIntent(ActivityIntent.SubmitForm(userId, teamId))
                },
                onCancel = ::closeSheet,
                snackbarHostState = snackbarHostState
            )
        }
    }
    state.conflict?.let { conflict ->
        ActivityConflictSheet(
            conflict = conflict,
            isSaving = state.isSaving,
            onResolve = { onIntent(ActivityIntent.ResolveConflict(it)) },
            onDismiss = if (conflict.dismissible) ({ onIntent(ActivityIntent.DismissConflict) }) else null
        )
    }

}

private val previewActivities = listOf(
    Activity(
        id = "a1", userId = "u1", teamId = "t1", title = "Weekend Trail Walk", description = null,
        activityType = "hiking", distanceMeters = 5400.0, durationSeconds = 3600.0, elevationGainMeters = 150.0,
        fitFilePath = null, startTime = 1_768_435_200_000L, endTime = 1_768_438_800_000L, isManualEntry = true,
        createdAt = 0L, updatedAt = 0L
    )
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Tablet", widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun FeedScreenPreview() {
    WandrTheme {
        FeedScreenContent(
            state = ActivityState(activities = previewActivities),
            userId = "u1", teamId = "t1", onIntent = {}, onOpenActivity = {}, onRecord = {}
        )
    }
}

@Preview(name = "Empty", showBackground = true)
@Preview(name = "Empty Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FeedScreenEmptyPreview() {
    WandrTheme {
        FeedScreenContent(
            state = ActivityState(), userId = "u1", teamId = null, onIntent = {}, onOpenActivity = {}, onRecord = {}
        )
    }
}

/** Whose entries the feed shows. */
enum class FeedScope(@StringRes val labelRes: Int) {
    Mine(R.string.activity_feed_mine),
    Team(R.string.activity_feed_team)
}
