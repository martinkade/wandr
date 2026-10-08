package com.wandr.android.ui.team

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.wandr.android.ui.common.AvatarImage
import com.wandr.android.ui.common.LocalHeroTransition
import com.wandr.android.ui.common.heroSource
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Team

@Composable
fun TeamCard(
    team: Team,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hero = LocalHeroTransition.current
    val heroKey = teamAvatarHeroKey(team.id)
    Row(
        modifier = modifier
            .background(
                color = CardDefaults.cardColors().containerColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .clickable {
                // Remember where the avatar is, so it can fly to the details page.
                hero.begin(heroKey) { size ->
                    AvatarImage(
                        avatarUrl = team.avatarUrl,
                        displayName = team.name,
                        size = size
                    )
                }
                onSelect()
            },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            avatarUrl = team.avatarUrl,
            displayName = team.name,
            modifier = Modifier.heroSource(heroKey)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = team.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            team.description?.let { description ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.team_invite_code, team.inviteCode),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

private val sampleTeam = Team(
    id = "t1",
    name = "Alpine Trail Blazers",
    description = "Hiking team for weekend trips.",
    avatarUrl = null,
    coverUrl = null,
    inviteCode = "X7K9P2W1",
    createdBy = "u1",
    createdAt = 0L,
    updatedAt = 0L
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun TeamCardPreview() {
    WandrTheme { TeamCard(team = sampleTeam, onSelect = {}, modifier = Modifier.padding(16.dp)) }
}
