package com.wandr.android.ui.activity.recording

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wandr.android.R
import com.wandr.android.ui.activity.InteractiveOsmMap
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.GpsTrackpoint

/**
 * The full-screen backdrop of the recording: the route so far on an OpenStreetMap background that can be dragged and zoomed. It is shown once the GPS is connected
 * ([isGpsConnected]); until then only a hint is shown.
 */
@Composable
fun RecordingMap(
    route: List<GpsTrackpoint>,
    isGpsConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        if (isGpsConnected) {
            // Drag and pinch; it follows the current position until the user moves it (then a button brings it back).
            InteractiveOsmMap(route = route, position = route.lastOrNull(), modifier = Modifier.fillMaxSize())
        }
        if (!isGpsConnected || route.isEmpty()) {
            Text(
                text = stringResource(R.string.recording_waiting_for_gps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun RecordingMapPreview() {
    WandrTheme {
        RecordingMap(
            listOf(
                GpsTrackpoint(47.3769, 8.5417, 400.0, 0L), GpsTrackpoint(47.3779, 8.5437, 400.0, 0L),
                GpsTrackpoint(47.3789, 8.5427, 400.0, 0L), GpsTrackpoint(47.3801, 8.5460, 400.0, 0L)
            ),
            isGpsConnected = true
        )
    }
}

@Preview(name = "Waiting", showBackground = true)
@Composable
private fun RecordingMapWaitingPreview() {
    WandrTheme { RecordingMap(emptyList(), isGpsConnected = false) }
}
