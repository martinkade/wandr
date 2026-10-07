package com.wandr.android.ui.activity

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.wandr.android.R
import com.wandr.android.location.FusedLocationSource
import com.wandr.android.location.toTrackpoint
import com.wandr.android.service.LocationTrackingService
import com.wandr.android.ui.activity.recording.GpsStatusBox
import com.wandr.android.ui.activity.recording.GrowFromBounds
import com.wandr.android.ui.activity.recording.RecordingControls
import com.wandr.android.ui.activity.recording.RecordingFinishDialog
import com.wandr.android.ui.activity.recording.RecordingMap
import com.wandr.android.ui.activity.recording.RecordingOverlay
import com.wandr.android.ui.permission.rememberPermissionGate
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.di.RecordingScope
import com.wandr.presentation.activity.ActivityIntent
import com.wandr.presentation.activity.ActivityState
import com.wandr.presentation.activity.ActivitySuccess
import com.wandr.presentation.activity.ActivityTypes
import com.wandr.presentation.activity.ActivityViewModel
import com.wandr.presentation.activity.GpsStatus
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Records an activity: the route over a map backdrop, a GPS status box that expands to a full-screen data overlay, and
 * start / pause / finish controls. The recording lives in a process-wide view model fed by a foreground service, so it
 * survives rotation and leaving the app. Leaving this screen is blocked while recording; a finished recording closes it.
 */
@Composable
fun LiveGpsTrackingScreen(
    userId: String,
    teamId: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = koinInject(RecordingScope)
) {
    val state by viewModel.state.collectAsState()
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
    modifier: Modifier = Modifier,
    /** Previews have no location permission and no service. */
    isPreview: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activityType by rememberSaveable { mutableStateOf(ActivityTypes.all.first()) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var finishing by rememberSaveable { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(isPreview) }
    // Before the start the screen shows the position; while recording the route comes from the view model.
    var currentLocation by remember { mutableStateOf<com.wandr.domain.model.GpsTrackpoint?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val blockedText = stringResource(R.string.activity_tracking_back_blocked)
    val recordingTitle = activityTypeText(state.trackingActivityType.takeIf { state.isTracking } ?: activityType)

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
    BackHandler(onBack = { if (expanded) expanded = false else tryLeave() })

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
    if (!isPreview) {
        // Ask when the screen opens, so the GPS fix is ready when the user taps start.
        LaunchedEffect(Unit) { locationPermission.request { permissionGranted = true } }

        // Before the start: only watch the GPS (status and position). While recording the foreground service does it.
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(permissionGranted, state.isTracking) {
            if (!permissionGranted || state.isTracking) return@LaunchedEffect
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                runCatching {
                    FusedLocationSource(context).updates(PREVIEW_INTERVAL_MILLIS).collect { location ->
                        onIntent(ActivityIntent.GpsFixChanged(location.accuracy))
                        currentLocation = location.toTrackpoint()
                    }
                }
            }
        }
    }

    fun start() {
        onIntent(ActivityIntent.StartGpsTracking(activityType))
        if (!isPreview) ContextCompat.startForegroundService(context, Intent(context, LocationTrackingService::class.java))
    }

    // 0 = collapsed (just the box), 1 = expanded (full screen); the expanded view grows out of the box's bounds.
    val expandProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "expandProgress"
    )
    var boxBounds by remember { mutableStateOf(Rect.Zero) }

    val route = if (state.isTracking) state.liveTrackpoints else listOfNotNull(currentLocation)

    Box(modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier
                .weight(1f)
                .fillMaxSize()) {
                // The map appears as soon as the GPS is connected; until then only the hint is shown.
                RecordingMap(route, isGpsConnected = state.gpsStatus != GpsStatus.SEARCHING)
                IconButton(
                    onClick = ::tryLeave,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.recording_leave)
                    )
                }
                GpsStatusBox(
                    state, onExpand = { expanded = true }, modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .onGloballyPositioned { boxBounds = it.boundsInParent() }
                )
                SnackbarHost(snackbarHostState, Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding())

                // Grows out of the bounds of the box and shrinks back into them.
                if (expandProgress > 0f) {
                    GrowFromBounds(
                        expandProgress,
                        from = boxBounds,
                        fromCornerRadius = with(LocalDensity.current) { 16.dp.toPx() }) {
                        RecordingOverlay(state, onCollapse = { expanded = false })
                    }
                }
            }
            RecordingControls(
                activityType = if (state.isTracking) state.trackingActivityType else activityType,
                isTracking = state.isTracking,
                isPaused = state.isPaused,
                onTypeChange = { activityType = it },
                onPrimaryAction = {
                    when {
                        !state.isTracking -> if (permissionGranted) start() else locationPermission.request {
                            permissionGranted = true
                            start()
                        }
                        state.isPaused -> onIntent(ActivityIntent.ResumeGpsTracking)
                        else -> onIntent(ActivityIntent.PauseGpsTracking)
                    }
                },
                onFinish = {
                    onIntent(ActivityIntent.PauseGpsTracking) // the clock stops while the user decides
                    finishing = true
                }
            )
        }
    }

    if (finishing) {
        RecordingFinishDialog(
            onSave = {
                finishing = false
                onIntent(ActivityIntent.StopAndSaveGpsTracking(userId, teamId, recordingTitle))
            },
            onDiscard = {
                finishing = false
                onIntent(ActivityIntent.DiscardRecording)
                onClose()
            },
            onContinue = {
                finishing = false
                onIntent(ActivityIntent.ResumeGpsTracking)
            }
        )
    }
}

private const val PREVIEW_INTERVAL_MILLIS = 2_000L

@Preview(name = "Idle", showBackground = true, heightDp = 800)
@Preview(name = "Idle Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, heightDp = 800)
@Composable
private fun LiveGpsTrackingScreenIdlePreview() {
    WandrTheme { LiveGpsTrackingScreenContent(ActivityState(), "u1", null, onIntent = {}, onClose = {}, isPreview = true) }
}

@Preview(name = "Recording", showBackground = true, heightDp = 800)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 1000)
@Composable
private fun LiveGpsTrackingScreenRecordingPreview() {
    WandrTheme {
        LiveGpsTrackingScreenContent(
            ActivityState(
                isTracking = true, gpsAccuracyMeters = 6f, liveDistanceMeters = 3_450.0, liveDurationSeconds = 1_240.0,
                liveCurrentPaceSecondsPerKm = 302.0,
                liveTrackpoints = listOf(
                    com.wandr.domain.model.GpsTrackpoint(47.3769, 8.5417, 400.0, 0L),
                    com.wandr.domain.model.GpsTrackpoint(47.3779, 8.5437, 400.0, 0L),
                    com.wandr.domain.model.GpsTrackpoint(47.3789, 8.5427, 400.0, 0L)
                )
            ),
            "u1", "t1", onIntent = {}, onClose = {}, isPreview = true
        )
    }
}
