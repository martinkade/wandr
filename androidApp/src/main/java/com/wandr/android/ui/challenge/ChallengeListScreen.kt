package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.domain.model.Challenge

@Composable
fun ChallengeListScreen(
    challenges: List<Challenge>,
    onSelectChallenge: (Challenge) -> Unit,
    onJoinChallenge: (Challenge) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.challenges_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(challenges) { challenge ->
                    ChallengeCard(
                        challenge = challenge,
                        onSelect = { onSelectChallenge(challenge) },
                        onJoin = { onJoinChallenge(challenge) },
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ChallengeListScreenPreview() {
    MaterialTheme {
        ChallengeListScreen(
            challenges = listOf(
                Challenge("c1", "t1", "30-Day 100km Hike", "Hike 100km total.", null, "group", "distance", 100000.0, true, 0L, 0L, "active", "u1", 0L, 0L),
                Challenge("c2", "t1", "5000m Altitude Climb", "Climb 5000m total.", null, "group", "elevation", 5000.0, false, 0L, 0L, "active", "u1", 0L, 0L)
            ),
            onSelectChallenge = {},
            onJoinChallenge = {}
        )
    }
}
