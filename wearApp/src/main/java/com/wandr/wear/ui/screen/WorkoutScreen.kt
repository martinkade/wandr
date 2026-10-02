package com.wandr.wear.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.wandr.wear.R
import com.wandr.wear.format.WorkoutFormat
import com.wandr.wear.model.LiveWorkoutState
import com.wandr.wear.model.WorkoutPhase
import com.wandr.wear.ui.WearPreviews
import com.wandr.wear.ui.component.WorkoutStat
import com.wandr.wear.ui.theme.WandrWearTheme

@Composable
fun WorkoutScreen(
    live: LiveWorkoutState,
    busy: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberScalingLazyListState()
    val paused = live.phase == WorkoutPhase.PAUSED
    val unavailable = stringResource(R.string.value_unavailable)
    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = WorkoutFormat.duration(live.elapsedMs),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )
            }
            if (paused) {
                item {
                    Text(stringResource(R.string.paused_label), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium)
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    WorkoutStat(
                        label = stringResource(R.string.label_distance),
                        value = stringResource(R.string.distance_km_format, WorkoutFormat.kilometers(live.distanceMeters))
                    )
                    WorkoutStat(
                        label = stringResource(R.string.label_heart_rate),
                        value = live.heartRate?.let { stringResource(R.string.heart_rate_format, it) } ?: unavailable
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (paused) {
                        Button(onClick = onResume, enabled = !busy, label = { Text(stringResource(R.string.resume)) })
                    } else {
                        FilledTonalButton(onClick = onPause, enabled = !busy, label = { Text(stringResource(R.string.pause)) })
                    }
                }
            }
            item {
                FilledTonalButton(onClick = onStop, enabled = !busy, label = { Text(stringResource(R.string.stop)) })
            }
        }
    }
}

@WearPreviews
@Composable
private fun WorkoutScreenPreview() {
    WandrWearTheme {
        WorkoutScreen(
            live = LiveWorkoutState(WorkoutPhase.ACTIVE, distanceMeters = 4321.0, heartRate = 132, elapsedMs = 2_345_000),
            busy = false, onPause = {}, onResume = {}, onStop = {}
        )
    }
}
