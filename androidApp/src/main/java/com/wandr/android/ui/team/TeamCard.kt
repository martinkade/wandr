package com.wandr.android.ui.team

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.AvatarImage
import com.wandr.android.ui.common.CoverHero
import com.wandr.android.ui.common.LocalHeroTransition
import com.wandr.android.ui.common.heroSource
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Team

/**
 * A team in the list, built like the challenge card and the team details: the cover (or elevation lines), then avatar,
 * name and description, and the invite code. Tapping opens the details; the avatar flies there (hero transition).
 */
@Composable
fun TeamCard(
    team: Team,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hero = LocalHeroTransition.current
    val heroKey = teamAvatarHeroKey(team.id)

    Column(
        modifier
            .background(
                color = CardDefaults.cardColors().containerColor,
                shape = RoundedCornerShape(16.dp)
            )
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
            }
            .clip(RoundedCornerShape(16.dp)),
    ) {
        CoverHero(
            coverUrl = team.coverUrl,
            seed = team.id.hashCode(),
            height = 128.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarImage(
                avatarUrl = team.avatarUrl,
                displayName = team.name,
                size = 64.dp,
                modifier = Modifier.heroSource(heroKey)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.team_invite_code, team.inviteCode),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        team.description?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
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
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 900)
@Composable
private fun TeamCardPreview() {
    WandrTheme {
        Surface {
            TeamCard(team = sampleTeam, onSelect = {})
        }
    }
}

@Preview(name = "Without description", showBackground = true)
@Composable
private fun TeamCardNoDescriptionPreview() {
    WandrTheme {
        Surface {
            TeamCard(
                team = sampleTeam.copy(description = null),
                onSelect = {},
            )
        }
    }
}
