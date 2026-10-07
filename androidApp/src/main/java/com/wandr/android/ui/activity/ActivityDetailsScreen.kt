package com.wandr.android.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.CollapsingHeaderScaffold
import com.wandr.android.ui.common.CoverHero
import com.wandr.android.ui.common.CoverHeroHeight
import com.wandr.android.ui.common.LabeledValue
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.common.SheetOverlap
import com.wandr.android.ui.common.userMessage
import com.wandr.android.ui.social.SocialSectionHost
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.util.AppDateFormatter
import com.wandr.domain.geo.PolylineCodec
import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.model.SocialEntityType
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
        modifier = modifier,
        socialSection = { activity ->
            SocialSectionHost(
                type = SocialEntityType.ACTIVITY,
                entityId = activity.id,
                userId = userId,
                entityOwnerId = activity.userId
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityDetailsScreenContent(
    state: ActivityState,
    userId: String,
    onBack: () -> Unit,
    onIntent: (ActivityIntent) -> Unit,
    modifier: Modifier = Modifier,
    socialSection: @Composable (Activity) -> Unit = {}
) {
    val activity = state.selectedActivity
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
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
    val errorText = state.error?.userMessage()
    LaunchedEffect(state.success, errorText) {
        if (state.success == ActivitySuccess.DELETED) {
            onIntent(ActivityIntent.ClearMessages)
            onBack() // the activity is gone
            return@LaunchedEffect
        }
        val message = when {
            errorText != null -> errorText
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

    // While the sheet is open, its own host shows the messages.
    val snackbarHost: @Composable () -> Unit = { if (!isEditing) SnackbarHost(snackbarHostState) }

    if (activity == null) {
        ScreenScaffold(
            title = stringResource(R.string.activity_details_title),
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
        val locale = LocalConfiguration.current.locales[0]
        // The full track from this device's FIT file, otherwise the simplified route from the server.
        val route = remember(state.selectedTrack, activity.polyline) {
            state.selectedTrack.ifEmpty { activity.polyline?.let(PolylineCodec::decode).orEmpty() }
        }
        val hasMap = route.size >= 2

        // The map is the header: it scrolls away with a parallax effect, the top bar turns solid and shows the title.
        CollapsingHeaderScaffold(
            title = activity.title,
            onBack = onBack,
            headerHeight = if (hasMap) MapHeaderHeight else CoverHeroHeight,
            modifier = modifier,
            header = {
                if (hasMap) {
                    OsmTrackMap(
                        trackpoints = route,
                        modifier = Modifier.fillMaxSize(),
                        // The route stays clear of the top bar and of the sheet that slides over the lower edge.
                        topPadding = 96.dp,
                        bottomPadding = 32.dp + SheetOverlap
                    )
                    // A scrim keeps the back button readable on any map.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                } else {
                    CoverHero(coverUrl = null, seed = activity.id.hashCode())
                }
            },
            actions = { contentColor ->
                if (state.canEdit) {
                    TextButton(
                        onClick = {
                            onIntent(ActivityIntent.StartEdit(activity.id))
                            isEditing = true
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
                    ) { Text(stringResource(R.string.profile_edit_button)) }
                }
            },
            snackbarHost = snackbarHost
        ) {
            Column(
                Modifier
                    .offset(y = -SheetOverlap)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = SheetOverlap, topEnd = SheetOverlap))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp)
            ) {
                if (hasMap) {
                    if (activity.userId == userId && !activity.showMap) {
                        Text(
                            text = stringResource(R.string.activity_map_hidden_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                } else if (!activity.isManualEntry) {
                    // No route for this user: recorded on another device, or its owner keeps the map private.
                    Text(
                        text = stringResource(R.string.activity_track_local_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }

                LabeledValue(stringResource(R.string.activity_title_label), activity.title)
                LabeledValue(
                    stringResource(R.string.activity_description_label),
                    activity.description
                )
                LabeledValue(
                    stringResource(R.string.activity_type_label),
                    activityTypeText(activity.activityType)
                )
                LabeledValue(
                    stringResource(R.string.challenge_start_label),
                    AppDateFormatter.formatDateTime(activity.startTime, locale = locale)
                )
                LabeledValue(
                    stringResource(R.string.distance_label),
                    String.format(Locale.US, "%.2f", activity.distanceMeters / 1000.0)
                )
                LabeledValue(
                    stringResource(R.string.duration_label),
                    "${(activity.durationSeconds / 60.0).toInt()}"
                )
                LabeledValue(
                    stringResource(R.string.elevation_label),
                    "${activity.elevationGainMeters.toInt()}"
                )
                LabeledValue(
                    stringResource(R.string.activity_source_label),
                    stringResource(if (activity.isManualEntry) R.string.manual_entry else R.string.gps_tracked)
                )
                if (state.canEdit) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.activity_delete_button)) }
                }
                Spacer(Modifier.height(16.dp))
                socialSection(activity)
            }
        }
    }

    if (confirmDelete && activity != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.activity_delete_confirm_title)) },
            text = { Text(stringResource(R.string.activity_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onIntent(ActivityIntent.DeleteActivity(activity.id))
                }) { Text(stringResource(R.string.social_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel_button)) } }
        )
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

/** The map header is taller than a cover: the route needs room. */
private val MapHeaderHeight = 320.dp

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
