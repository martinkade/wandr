package com.wandr.android.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
fun ManualActivityEntryScreen(
    state: ActivityState,
    userId: String,
    teamId: String?,
    onIntent: (ActivityIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.log_manual_activity_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.manualTitle,
            onValueChange = { onIntent(ActivityIntent.UpdateManualTitle(it)) },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.manualDescription,
            onValueChange = { onIntent(ActivityIntent.UpdateManualDescription(it)) },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.manualDistanceKm.toString(),
            onValueChange = { onIntent(ActivityIntent.UpdateManualDistance(it.toDoubleOrNull() ?: 0.0)) },
            label = { Text(stringResource(R.string.distance_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.manualDurationMinutes.toString(),
            onValueChange = { onIntent(ActivityIntent.UpdateManualDuration(it.toDoubleOrNull() ?: 0.0)) },
            label = { Text(stringResource(R.string.duration_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.manualElevationMeters.toString(),
            onValueChange = { onIntent(ActivityIntent.UpdateManualElevation(it.toDoubleOrNull() ?: 0.0)) },
            label = { Text(stringResource(R.string.elevation_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = { onIntent(ActivityIntent.SubmitManualActivity(userId, teamId)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_activity_button))
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ManualActivityEntryScreenPreview() {
    MaterialTheme {
        ManualActivityEntryScreen(
            state = ActivityState(manualTitle = "Evening Run"),
            userId = "u1",
            teamId = "t1",
            onIntent = {}
        )
    }
}
