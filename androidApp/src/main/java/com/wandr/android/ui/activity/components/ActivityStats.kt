package com.wandr.android.ui.activity.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.activity.ActivityFormat
import com.wandr.android.ui.common.LabeledValue
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Activity

/** Distance, pace (or speed on a bike), time and elevation gain. */
@Composable
fun ActivityStats(activity: Activity, modifier: Modifier = Modifier) {
    val isCycling = activity.activityType == "cycling"
    val pace =
        if (isCycling) ActivityFormat.speedKmh(activity.distanceMeters, activity.durationSeconds)
        else ActivityFormat.pace(activity.distanceMeters, activity.durationSeconds)

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LabeledValue(
            label = stringResource(R.string.activity_stat_distance),
            value = "${ActivityFormat.distanceKm(activity.distanceMeters)} km",
            modifier = Modifier.weight(1f)
        )
        LabeledValue(
            label = stringResource(R.string.activity_stat_time),
            value = ActivityFormat.duration(activity.durationSeconds),
            modifier = Modifier.weight(1f)
        )
        if (pace != null) {
            LabeledValue(
                label = stringResource(if (isCycling) R.string.activity_stat_speed else R.string.activity_stat_pace),
                value = if (isCycling) "$pace km/h" else "$pace /km",
                modifier = Modifier.weight(1f)
            )
        }
        if (activity.elevationGainMeters > 0) {
            LabeledValue(
                label = stringResource(R.string.activity_stat_elevation),
                value = "${activity.elevationGainMeters.toInt()} m",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private val previewActivity = Activity(
    id = "a1",
    userId = "u1",
    teamId = null,
    title = "Afternoon run",
    description = null,
    activityType = "running",
    distanceMeters = 9_160.0,
    durationSeconds = 3_067.0,
    elevationGainMeters = 85.0,
    fitFilePath = null,
    startTime = 1_768_435_200_000L,
    endTime = 1_768_438_267_000L,
    isManualEntry = false,
    createdAt = 0L,
    updatedAt = 0L
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 400)
@Preview(name = "Tablet", widthDp = 840, heightDp = 300, showBackground = true)
@Composable
private fun ActivityStatsPreview() {
    WandrTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ActivityStats(previewActivity)
                // On a bike: speed instead of pace; flat route: no elevation.
                ActivityStats(
                    previewActivity.copy(
                        activityType = "cycling",
                        elevationGainMeters = 0.0
                    )
                )
            }
        }
    }
}
