package com.wandr.android.ui.activity.recording

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.wandr.android.R
import com.wandr.android.ui.activity.ActivityFormat
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.presentation.activity.ActivityState
import com.wandr.presentation.activity.GpsStatus

/**
 * The collapsed data box over the map: GPS status with a button that expands to the full-screen data overlay, and the
 * three key figures (time, pace or speed, distance).
 */
@Composable
fun GpsStatusBox(state: ActivityState, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val isCycling = state.trackingActivityType == "cycling"
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(painterResource(R.drawable.ic_gps_fixed), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary)
                Text(
                    text = stringResource(state.gpsStatus.labelRes()),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onExpand) {
                    Icon(painterResource(R.drawable.ic_open_in_full), contentDescription = stringResource(R.string.recording_expand), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Figure(ActivityFormat.clock(state.liveDurationSeconds), stringResource(R.string.recording_time_label))
                Figure(
                    value = if (isCycling) ActivityFormat.speedFromPace(state.liveCurrentPaceSecondsPerKm) else ActivityFormat.paceOrDash(state.liveCurrentPaceSecondsPerKm),
                    label = stringResource(if (isCycling) R.string.recording_speed_label else R.string.recording_pace_label)
                )
                Figure(ActivityFormat.distanceKm(state.liveDistanceMeters), stringResource(R.string.recording_distance_label))
            }
        }
    }
}

@Composable
private fun Figure(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun GpsStatusBoxPreview() {
    WandrTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GpsStatusBox(ActivityState(isTracking = true, gpsAccuracyMeters = 5f, liveDurationSeconds = 1_503.0, liveDistanceMeters = 5_020.0, liveCurrentPaceSecondsPerKm = 299.0), onExpand = {})
            GpsStatusBox(ActivityState(), onExpand = {})
            GpsStatusBox(ActivityState(isTracking = true, gpsAccuracyMeters = 80f, trackingActivityType = "cycling", liveCurrentPaceSecondsPerKm = 120.0), onExpand = {})
        }
    }
}
