package com.wandr.android.ui.activity.recording

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wandr.android.R
import com.wandr.android.ui.activity.ActivityFormat
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.geo.movingHeading
import com.wandr.presentation.activity.ActivityState

/**
 * The expanded data view: a full-screen overlay over the map with the clock, GPS status, the compass bar, and the big figures.
 * [onCollapse] returns to the map.
 */
@Composable
fun RecordingOverlay(state: ActivityState, onCollapse: () -> Unit, modifier: Modifier = Modifier) {
    val isCycling = state.trackingActivityType == "cycling"
    Column(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Box(Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .padding(bottom = 16.dp)) {
            IconButton(onClick = onCollapse, modifier = Modifier.align(Alignment.TopStart)) {
                Icon(
                    painterResource(R.drawable.ic_close_fullscreen),
                    contentDescription = stringResource(R.string.recording_collapse),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(Modifier
                .align(Alignment.Center)
                .padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(state.gpsStatus.labelRes()), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary)
                Text(
                    text = ActivityFormat.clock(state.liveDurationSeconds),
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, fontSize = 56.sp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Compass bar showing moving direction right below the yellow header
        CompassBar(
            heading = state.liveTrackpoints.movingHeading(),
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BigFigure(
                value = if (isCycling) ActivityFormat.speedFromPace(state.liveCurrentPaceSecondsPerKm) else ActivityFormat.paceOrDash(state.liveCurrentPaceSecondsPerKm),
                label = stringResource(if (isCycling) R.string.recording_speed_label else R.string.recording_pace_label)
            )
            BigFigure(ActivityFormat.distanceKm(state.liveDistanceMeters), stringResource(R.string.recording_distance_label))
        }
    }
}

@Composable
private fun BigFigure(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, fontSize = 72.sp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Preview(name = "Light Mode", showBackground = true, heightDp = 700)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, heightDp = 700)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 900)
@Composable
private fun RecordingOverlayPreview() {
    WandrTheme {
        RecordingOverlay(
            ActivityState(
                isTracking = true,
                gpsAccuracyMeters = 5f,
                liveDurationSeconds = 1_503.0,
                liveDistanceMeters = 5_020.0,
                liveCurrentPaceSecondsPerKm = 299.0,
                liveSplitsSeconds = listOf(305.0, 298.0, 301.0, 296.0, 299.0)
            ),
            onCollapse = {}
        )
    }
}

@Preview(name = "Before start", showBackground = true, heightDp = 700)
@Composable
private fun RecordingOverlayIdlePreview() {
    WandrTheme { RecordingOverlay(ActivityState(), onCollapse = {}) }
}
