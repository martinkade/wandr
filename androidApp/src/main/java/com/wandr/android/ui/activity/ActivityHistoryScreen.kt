package com.wandr.android.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Activity
import com.wandr.presentation.activity.ActivityIntent
import com.wandr.presentation.activity.ActivityState
import com.wandr.presentation.activity.ActivityViewModel
import org.koin.compose.koinInject

@Composable
fun ActivityHistoryScreen(
    userId: String,
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(userId) { viewModel.processIntent(ActivityIntent.LoadUserActivities(userId)) }
    ActivityHistoryScreenContent(
        state = state,
        onIntent = viewModel::processIntent,
        onSelectActivity = {},
        modifier = modifier
    )
}

@Composable
private fun ActivityHistoryScreenContent(
    state: ActivityState,
    onIntent: (ActivityIntent) -> Unit,
    onSelectActivity: (Activity) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(title = stringResource(R.string.activities_title), modifier = modifier) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (state.activities.isEmpty()) {
            Text(
                text = "No activities recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(state.activities) { activity ->
                    ActivityCard(
                        activity = activity,
                        onClick = { onSelectActivity(activity) }
                    )
                }
            }
        }
    }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ActivityHistoryScreenPreview() {
    WandrTheme {
        ActivityHistoryScreenContent(
            state = ActivityState(
                activities = listOf(
                    Activity(
                        id = "a1",
                        userId = "u1",
                        teamId = "t1",
                        title = "Weekend Trail Walk",
                        description = null,
                        activityType = "hiking",
                        distanceMeters = 5400.0,
                        durationSeconds = 3600.0,
                        elevationGainMeters = 150.0,
                        fitFilePath = null,
                        startTime = 0L,
                        endTime = 0L,
                        isManualEntry = true,
                        createdAt = 0L,
                        updatedAt = 0L
                    )
                )
            ),
            onIntent = {},
            onSelectActivity = {}
        )
    }
}
