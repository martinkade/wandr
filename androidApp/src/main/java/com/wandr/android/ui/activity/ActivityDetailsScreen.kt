package com.wandr.android.ui.activity

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.LabeledValue
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.util.AppDateFormatter
import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.presentation.activity.ActivityIntent
import com.wandr.presentation.activity.ActivityState
import com.wandr.presentation.activity.ActivitySuccess
import com.wandr.presentation.activity.ActivityViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Locale

/**
 * Read-only activity details with the recorded track (when it is on this device). Only the owner of the activity
 * sees "Edit", which opens [ActivityEditScreen] in a bottom sheet.
 */
@Composable
fun ActivityDetailsScreen(
    activityId: String,
    userId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(activityId, userId) {
        viewModel.processIntent(ActivityIntent.LoadUserActivities(userId)) // establishes the signed-in user
        viewModel.processIntent(ActivityIntent.SelectActivity(activityId))
    }
    ActivityDetailsScreenContent(
        state = state,
        userId = userId,
        onBack = onBack,
        onIntent = viewModel::processIntent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityDetailsScreenContent(
    state: ActivityState,
    userId: String,
    onBack: () -> Unit,
    onIntent: (ActivityIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = state.selectedActivity
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
            onIntent(ActivityIntent.DiscardForm)
        }
    }

    val updatedText = stringResource(R.string.activity_updated_message)
    val mergedText = stringResource(R.string.activity_merged_message)
    val trimmedText = stringResource(R.string.activity_trimmed_message)
    val discardedText = stringResource(R.string.activity_discarded_message)
    LaunchedEffect(state.success, state.errorMessage) {
        val message = when {
            state.errorMessage != null -> state.errorMessage
            state.success == ActivitySuccess.UPDATED -> updatedText
            state.success == ActivitySuccess.MERGED -> mergedText
            state.success == ActivitySuccess.TRIMMED -> trimmedText
            state.success == ActivitySuccess.DISCARDED -> discardedText
            else -> null
        } ?: return@LaunchedEffect

        if (state.success != null && saveRequested) closeSheet()
        saveRequested = false
        // Own scope: clearing the message changes the effect keys, which would cancel (and hide) the snackbar.
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
        onIntent(ActivityIntent.ClearMessages)
    }

    ScreenScaffold(
        title = stringResource(R.string.activity_details_title),
        modifier = modifier,
        onBack = onBack,
        actions = {
            if (state.canEdit && activity != null) {
                TextButton(onClick = {
                    onIntent(ActivityIntent.StartEdit(activity.id))
                    isEditing = true
                }) { Text(stringResource(R.string.profile_edit_button)) }
            }
        },
        // While the sheet is open, its own host shows the messages.
        snackbarHost = { if (!isEditing) SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (activity == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                val locale = LocalConfiguration.current.locales[0]
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                    if (state.selectedTrack.isNotEmpty()) {
                        TrackMapView(trackpoints = state.selectedTrack, modifier = Modifier.fillMaxWidth().height(200.dp))
                        Spacer(Modifier.height(8.dp))
                    } else if (!activity.isManualEntry) {
                        // The FIT file never leaves the recording device.
                        Text(
                            text = stringResource(R.string.activity_track_local_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    LabeledValue(stringResource(R.string.activity_title_label), activity.title)
                    LabeledValue(stringResource(R.string.activity_description_label), activity.description)
                    LabeledValue(stringResource(R.string.activity_type_label), activityTypeText(activity.activityType))
                    LabeledValue(
                        stringResource(R.string.challenge_start_label),
                        AppDateFormatter.formatDateTime(activity.startTime, locale = locale)
                    )
                    LabeledValue(stringResource(R.string.distance_label), String.format(Locale.US, "%.2f", activity.distanceMeters / 1000.0))
                    LabeledValue(stringResource(R.string.duration_label), "${(activity.durationSeconds / 60.0).toInt()}")
                    LabeledValue(stringResource(R.string.elevation_label), "${activity.elevationGainMeters.toInt()}")
                    LabeledValue(
                        stringResource(R.string.activity_source_label),
                        stringResource(if (activity.isManualEntry) R.string.manual_entry else R.string.gps_tracked)
                    )
                }
            }
        }
    }

    val form = state.form
    if (isEditing && form != null) {
        ModalBottomSheet(
            onDismissRequest = {
                onIntent(ActivityIntent.DiscardForm)
                isEditing = false
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
                    onIntent(ActivityIntent.SubmitForm(userId, activity?.teamId))
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

private val previewActivity = Activity(
    id = "a1", userId = "u1", teamId = null, title = "Evening run", description = "Along the river.",
    activityType = "running", distanceMeters = 10_300.0, durationSeconds = 3000.0, elevationGainMeters = 85.0,
    fitFilePath = "/files/fit/a1.fit", startTime = 1_768_435_200_000L, endTime = 1_768_438_200_000L,
    isManualEntry = false, createdAt = 0L, updatedAt = 0L
)

@Preview(name = "Owner with track", showBackground = true, heightDp = 1000)
@Preview(name = "Owner Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, heightDp = 1000)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 1400)
@Composable
private fun ActivityDetailsScreenOwnerPreview() {
    WandrTheme {
        ActivityDetailsScreenContent(
            state = ActivityState(
                selectedActivity = previewActivity, canEdit = true,
                selectedTrack = listOf(
                    GpsTrackpoint(47.3769, 8.5417, 400.0, 0L),
                    GpsTrackpoint(47.3779, 8.5437, 410.0, 0L),
                    GpsTrackpoint(47.3789, 8.5427, 420.0, 0L)
                )
            ),
            userId = "u1", onBack = {}, onIntent = {}
        )
    }
}

@Preview(name = "Other user, track on another device", showBackground = true, heightDp = 900)
@Composable
private fun ActivityDetailsScreenOtherUserPreview() {
    WandrTheme {
        ActivityDetailsScreenContent(
            state = ActivityState(selectedActivity = previewActivity, canEdit = false),
            userId = "u2", onBack = {}, onIntent = {}
        )
    }
}
