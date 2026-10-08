package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.ui.common.AvatarImage
import com.wandr.android.ui.common.sectionItemShape
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.ChallengeType
import com.wandr.domain.model.TeamStanding

/**
 * One team in the team-vs-team ranking of a team challenge, a card of a list of connected cards
 * ([isFirstItemInSection] / [isLastItemInSection] round the ends). The user's own team ([isMine]) is highlighted, so it is
 * easy to compare it with the other teams.
 *
 * @param showMemberProgress "3 of 5 members reached the goal"; only for all-or-nothing challenges, where it decides
 *   whether the team completes
 */
@Composable
fun TeamStandingRow(
    standing: TeamStanding,
    type: ChallengeType,
    isFirstItemInSection: Boolean,
    isLastItemInSection: Boolean,
    modifier: Modifier = Modifier,
    isMine: Boolean = false,
    showMemberProgress: Boolean = false
) {
    val itemShape = sectionItemShape(isFirstItemInSection, isLastItemInSection)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isMine) MaterialTheme.colorScheme.primaryContainer else CardDefaults.cardColors().containerColor,
                shape = itemShape
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "#${standing.rank}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        AvatarImage(avatarUrl = standing.avatarUrl, displayName = standing.teamName, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = standing.teamName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isMine) FontWeight.Bold else FontWeight.Normal),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1.0f)
                )
                Text(
                    challengeValueText(type, standing.totalProgress),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = {
                        if (showMemberProgress) {
                            (standing.completedMemberCount / standing.memberCount.toFloat()).coerceIn(
                                0f,
                                1f
                            )
                        } else {
                            (standing.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f)
                        }
                    },
                    modifier = Modifier
                        .weight(1.0f)
                )
                Text(
                    if (showMemberProgress) "${standing.completedMemberCount} / ${standing.memberCount}" else "${standing.progressPercentage.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun TeamStandingRowPreview() {
    WandrTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TeamStandingRow(
                TeamStanding(1, "t2", "City Runners", null, 80_000.0, 80.0, 4, 1, false),
                ChallengeType.DISTANCE,
                isFirstItemInSection = true,
                isLastItemInSection = false,
                isMine = true,
                showMemberProgress = true
            )
            TeamStandingRow(
                TeamStanding(2, "t1", "Alpine Trail Blazers", null, 50_000.0, 50.0, 3, 0, false),
                ChallengeType.DISTANCE,
                isFirstItemInSection = false,
                isLastItemInSection = true,
                showMemberProgress = true
            )
        }
    }
}
