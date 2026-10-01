package com.wandr.android.ui.activity

import android.content.res.Configuration
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.presentation.activity.ActivityIntent
import com.wandr.presentation.activity.ActivityState

@Composable
fun LiveGpsTrackingScreen(
    state: ActivityState,
    userId: String,
    teamId: String?,
    onIntent: (ActivityIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.live_gps_tracking_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        TrackMapView(trackpoints = state.liveTrackpoints)

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DIST (KM)", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "%.2f".format(state.liveDistanceMeters / 1000.0),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TIME (MIN)", style = MaterialTheme.typography.labelMedium)
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
                onClick = { onIntent(ActivityIntent.StartGpsTracking("hiking")) },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.start_tracking_button))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (state.isPaused) onIntent(ActivityIntent.ResumeGpsTracking)
                        else onIntent(ActivityIntent.PauseGpsTracking)
                    },
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text(if (state.isPaused) stringResource(R.string.resume_button) else stringResource(R.string.pause_button))
                }

                Button(
                    onClick = { onIntent(ActivityIntent.StopAndSaveGpsTracking(userId, teamId, "GPS Tracked Activity")) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text(stringResource(R.string.stop_save_button))
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun LiveGpsTrackingScreenPreview() {
    MaterialTheme {
        LiveGpsTrackingScreen(
            state = ActivityState(
                isTracking = true,
                liveDistanceMeters = 3450.0,
                liveDurationSeconds = 1240.0
            ),
            userId = "u1",
            teamId = "t1",
            onIntent = {}
        )
    }
}
