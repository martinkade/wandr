package com.wandr.wear.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.wandr.domain.watch.WatchWorkout
import com.wandr.wear.R
import com.wandr.wear.format.WorkoutFormat
import com.wandr.wear.model.SendState
import com.wandr.wear.ui.WearPreviews
import com.wandr.wear.ui.component.WorkoutStat
import com.wandr.wear.ui.theme.WandrWearTheme

@Composable
fun SummaryScreen(
    workout: WatchWorkout,
    sendState: SendState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberScalingLazyListState()
    val unavailable = stringResource(R.string.value_unavailable)
    val stateRes = when (sendState) {
        SendState.PENDING -> R.string.send_state_pending
        SendState.SENT -> R.string.send_state_sent
        SendState.DELIVERED -> R.string.send_state_delivered
    }
    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        ScalingLazyColumn(state = listState, contentPadding = contentPadding, horizontalAlignment = Alignment.CenterHorizontally) {
            item { ListHeader { Text(stringResource(R.string.summary_title)) } }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    WorkoutStat(
                        label = stringResource(R.string.label_duration),
                        value = WorkoutFormat.duration(workout.endTime - workout.startTime)
                    )
                    WorkoutStat(
                        label = stringResource(R.string.label_distance),
                        value = stringResource(R.string.distance_km_format, WorkoutFormat.kilometers(workout.distanceMeters))
                    )
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    WorkoutStat(
                        label = stringResource(R.string.label_elevation),
                        value = stringResource(R.string.elevation_format, workout.elevationGainMeters.toInt())
                    )
                    WorkoutStat(
                        label = stringResource(R.string.label_avg_heart_rate),
                        value = workout.averageHeartRate?.let { stringResource(R.string.heart_rate_format, it) } ?: unavailable
                    )
                }
            }
            item {
                Text(
                    text = stringResource(stateRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (sendState == SendState.DELIVERED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            item { Button(onClick = onDone, label = { Text(stringResource(R.string.done)) }) }
        }
    }
}

@WearPreviews
@Composable
private fun SummaryScreenPreview() {
    WandrWearTheme {
        SummaryScreen(
            workout = WatchWorkout("id", "hiking", 0, 3_725_000, 8210.0, 240.0, 128, 161),
            sendState = SendState.SENT,
            onDone = {}
        )
    }
}
