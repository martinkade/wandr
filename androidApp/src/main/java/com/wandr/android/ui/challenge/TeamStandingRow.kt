package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.ChallengeType
import com.wandr.domain.model.TeamStanding

/** One team in the team-vs-team ranking of a group challenge. */
@Composable
fun TeamStandingRow(
    standing: TeamStanding,
    type: ChallengeType,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "${standing.rank}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.width(32.dp)
        )
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = standing.teamName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = challengeValueText(type, standing.totalProgress),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (standing.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.challenge_standings_members, standing.completedMemberCount, standing.memberCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun TeamStandingRowPreview() {
    WandrTheme {
        Column(Modifier.padding(16.dp)) {
            TeamStandingRow(TeamStanding(1, "t2", "City Runners", null, 80_000.0, 80.0, 4, 1, false), ChallengeType.DISTANCE)
            TeamStandingRow(TeamStanding(2, "t1", "Alpine Trail Blazers", null, 50_000.0, 50.0, 3, 0, false), ChallengeType.DISTANCE)
        }
    }
}
