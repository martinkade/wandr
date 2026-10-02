package com.wandr.android.ui.activity

import android.Manifest
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.permission.rememberPermissionGate
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.presentation.activity.ActivityIntent
import com.wandr.presentation.activity.ActivityState
import com.wandr.presentation.activity.ActivitySuccess
import com.wandr.presentation.activity.ActivityTypes
import com.wandr.presentation.activity.ActivityViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Records an activity. Leaving the screen while recording would lose the track, so back is blocked until the
 * recording is stopped and saved. A saved recording closes the screen via [onClose].
 */
@Composable
fun LiveGpsTrackingScreen(
    userId: String,
    teamId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(userId) { viewModel.processIntent(ActivityIntent.LoadUserActivities(userId)) }
    LaunchedEffect(state.success) {
        if (state.success == ActivitySuccess.RECORDED || state.success == ActivitySuccess.DISCARDED ||
            state.success == ActivitySuccess.MERGED || state.success == ActivitySuccess.TRIMMED
        ) {
            viewModel.processIntent(ActivityIntent.ClearMessages)
            onClose()
        }
    }
    LiveGpsTrackingScreenContent(
        state = state,
        userId = userId,
        teamId = teamId,
        onIntent = viewModel::processIntent,
        onClose = onClose,
        modifier = modifier
    )
}

@Composable
private fun LiveGpsTrackingScreenContent(
    state: ActivityState,
    userId: String,
    teamId: String?,
    onIntent: (ActivityIntent) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activityType by rememberSaveable { mutableStateOf(ActivityTypes.all.first()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val blockedText = stringResource(R.string.activity_tracking_back_blocked)
    val defaultTitle = stringResource(R.string.live_gps_tracking_title)
    // Title of a recorded activity until the owner renames it from the details screen.
    val recordingTitle = activityTypeText(activityType)

    fun tryLeave() {
        if (state.isTracking) {
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(blockedText)
            }
        } else {
            onClose()
        }
    }
    BackHandler(onBack = ::tryLeave)

    // Disclosure first, then the system dialog; the foreground-service notification is optional (Android 13+).
    val locationPermission = rememberPermissionGate(
        required = listOf(Manifest.permission.ACCESS_FINE_LOCATION),
        optional = buildList {
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        },
        disclosureTitle = R.string.permission_location_title,
        disclosureMessage = R.string.permission_location_message,
        deniedMessage = R.string.permission_location_denied
    )

    ScreenScaffold(
        title = defaultTitle,
        modifier = modifier,
        onBack = ::tryLeave,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!state.isTracking) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActivityTypes.all.forEach { type ->
                        FilterChip(
                            selected = type == activityType,
                            onClick = { activityType = type },
                            label = { Text(activityTypeText(type)) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            TrackMapView(trackpoints = state.liveTrackpoints)

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.distance_label), style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "%.2f".format(state.liveDistanceMeters / 1000.0),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.duration_label), style = MaterialTheme.typography.labelMedium)
                    val mins = (state.liveDurationSeconds / 60.0).toInt()
                    val secs = (state.liveDurationSeconds % 60.0).toInt()
                    Text(
                        text = "%02d:%02d".format(mins, secs),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (!state.isTracking) {
                Button(
                    onClick = { locationPermission.request { onIntent(ActivityIntent.StartGpsTracking(activityType)) } },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(stringResource(R.string.start_tracking_button))
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (state.isPaused) onIntent(ActivityIntent.ResumeGpsTracking)
                            else onIntent(ActivityIntent.PauseGpsTracking)
                        },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text(stringResource(if (state.isPaused) R.string.resume_button else R.string.pause_button))
                    }
                    Button(
                        onClick = { onIntent(ActivityIntent.StopAndSaveGpsTracking(userId, teamId, recordingTitle)) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text(stringResource(R.string.stop_save_button))
                    }
                }
            }
        }
    }

    state.conflict?.let { conflict ->
        ActivityConflictSheet(
            conflict = conflict,
            isSaving = state.isSaving,
            onResolve = { onIntent(ActivityIntent.ResolveConflict(it)) },
            onDismiss = null // a finished recording must be resolved, otherwise it would be lost
        )
    }
}

@Preview(name = "Tracking", showBackground = true)
@Preview(name = "Tracking Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun LiveGpsTrackingScreenPreview() {
    WandrTheme {
        LiveGpsTrackingScreenContent(
            state = ActivityState(isTracking = true, liveDistanceMeters = 3450.0, liveDurationSeconds = 1240.0),
            userId = "u1", teamId = "t1", onIntent = {}, onClose = {}
        )
    }
}

@Preview(name = "Idle", showBackground = true)
@Composable
private fun LiveGpsTrackingScreenIdlePreview() {
    WandrTheme {
        LiveGpsTrackingScreenContent(state = ActivityState(), userId = "u1", teamId = null, onIntent = {}, onClose = {})
    }
}
