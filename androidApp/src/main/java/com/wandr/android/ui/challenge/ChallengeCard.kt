package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.R
import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeStatus

@Composable
fun ChallengeCard(
    challenge: Challenge,
    /** Derived at runtime from the dates, never stored (see `EvaluateChallengeStatusUseCase`). */
    status: ChallengeStatus,
    onSelect: () -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
    /** Shown only to managers who may edit this challenge. */
    onEdit: (() -> Unit)? = null,
    /** Group challenges: shown when the user belongs to a team that could be enrolled. */
    onEnrollTeam: (() -> Unit)? = null
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
                    text = stringResource(statusLabel(status)).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
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

            val isGroup = challenge.scope == "group"
            if (isGroup) {
                Text(
                    text = stringResource(R.string.challenge_group_card_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isGroup) {
                    // Teams compete as a team: enrolling replaces "join".
                    onEnrollTeam?.let {
                        Button(onClick = it, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.challenge_enroll_team_button))
                        }
                    } ?: Spacer(Modifier.weight(1f))
                } else {
                    Button(onClick = onJoin, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.join_challenge_button))
                    }
                }
                onEdit?.let {
                    OutlinedButton(onClick = it) { Text(stringResource(R.string.challenge_edit_button)) }
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun ChallengeCardPreview() {
    WandrTheme {
        ChallengeCard(
            challenge = Challenge(
                id = "c1",
                title = "30-Day 100km Hike",
                description = "Hike 100km total within 30 days.",
                coverUrl = null,
                scope = "group",
                type = "distance",
                targetValue = 100000.0,
                requireAllMembersCompletion = true,
                startDate = 0L,
                endDate = 0L,
                createdBy = "u1",
                createdAt = 0L,
                updatedAt = 0L
            ),
            status = ChallengeStatus.ACTIVE,
            onSelect = {},
            onJoin = {},
            onEdit = {} // as a manager
        )
    }
}

@Preview(name = "Group challenge", showBackground = true)
@Preview(name = "Group challenge Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ChallengeCardGroupPreview() {
    WandrTheme {
        ChallengeCard(
            challenge = Challenge(
                id = "c2", title = "Team Altitude Climb", description = "Teams compete for 5000 m.", coverUrl = null,
                scope = "group", type = "elevation", targetValue = 5000.0, requireAllMembersCompletion = true,
                startDate = 0L, endDate = 0L, createdBy = "u1", createdAt = 0L, updatedAt = 0L
            ),
            status = ChallengeStatus.ACTIVE,
            onSelect = {},
            onJoin = {},
            onEnrollTeam = {}
        )
    }
}

private fun statusLabel(status: ChallengeStatus) = when (status) {
    ChallengeStatus.DRAFT -> R.string.challenge_status_draft
    ChallengeStatus.PLANNED -> R.string.challenge_status_planned
    ChallengeStatus.ACTIVE -> R.string.challenge_status_active
    ChallengeStatus.COMPLETED -> R.string.challenge_status_completed
    ChallengeStatus.EXPIRED -> R.string.challenge_status_expired
}
