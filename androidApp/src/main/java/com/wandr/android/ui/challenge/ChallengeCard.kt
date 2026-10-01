package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.domain.model.Challenge

@Composable
fun ChallengeCard(
    challenge: Challenge,
    onSelect: () -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = challenge.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            challenge.description?.let { desc ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (challenge.requireAllMembersCompletion) {
                Text(
                    text = "🔒 " + stringResource(R.string.require_all_members_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onJoin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.join_challenge_button))
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ChallengeCardPreview() {
    MaterialTheme {
        ChallengeCard(
            challenge = Challenge(
                id = "c1",
                teamId = "t1",
                title = "30-Day 100km Hike",
                description = "Hike 100km total within 30 days.",
                coverUrl = null,
                scope = "group",
                type = "distance",
                targetValue = 100000.0,
                requireAllMembersCompletion = true,
                startDate = 0L,
                endDate = 0L,
                status = "active",
                createdBy = "u1",
                createdAt = 0L,
                updatedAt = 0L
            ),
            onSelect = {},
            onJoin = {}
        )
    }
}
