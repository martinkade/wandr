package com.wandr.android.ui.profile.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.AvatarImage
import com.wandr.android.ui.common.LocalHeroTransition
import com.wandr.android.ui.common.heroSource
import com.wandr.android.ui.common.sectionItemShape
import com.wandr.android.ui.team.teamAvatarHeroKey
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Team

/**
 * One team of the user. The first one (highest priority) is marked: it is the only team that counts for team
 * challenges. [dragHandleModifier] makes the handle the grip for drag and drop; without it (null) the item is not
 * reorderable and shows no handle. With [onClick] the item can be tapped.
 */
@Composable
fun TeamMembershipItem(
    team: Team,
    isPrimary: Boolean,
    isFirstItemInSection: Boolean,
    isLastItemInSection: Boolean,
    dragHandleModifier: Modifier?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val itemShape = sectionItemShape(isFirstItemInSection, isLastItemInSection)
    val hero = LocalHeroTransition.current
    val heroKey = teamAvatarHeroKey(team.id)
    Row(
        modifier
            .fillMaxWidth()
            .clip(itemShape)
            .let {
                if (onClick == null) it
                else it.clickable {
                    // Remember where the avatar is, so it can fly to the details page.
                    hero.begin(heroKey) { size ->
                        AvatarImage(
                            avatarUrl = team.avatarUrl,
                            displayName = team.name,
                            size = size
                        )
                    }
                    onClick()
                }
            }
            .background(
                color = CardDefaults.cardColors().containerColor,
                shape = itemShape
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            avatarUrl = team.avatarUrl,
            displayName = team.name,
            size = 40.dp,
            modifier = Modifier.heroSource(heroKey)
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = team.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isPrimary) {
                Text(
                    text = stringResource(R.string.memberships_primary),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.tertiary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (dragHandleModifier != null) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = stringResource(R.string.memberships_reorder),
                modifier = dragHandleModifier.padding(8.dp)
            )
        }
    }
}

private val previewTeam =
    Team("t1", "Alpine Trail Blazers", null, null, null, "X7K9P2W1", "u1", 0L, 0L)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun TeamMembershipItemPreview() {
    WandrTheme {
        Surface {
            Column {
                TeamMembershipItem(
                    previewTeam,
                    isPrimary = true,
                    isFirstItemInSection = true,
                    isLastItemInSection = false,
                    dragHandleModifier = Modifier
                )
                TeamMembershipItem(
                    previewTeam.copy(name = "City Runners"),
                    isFirstItemInSection = false,
                    isLastItemInSection = false,
                    isPrimary = false,
                    dragHandleModifier = Modifier
                )
                TeamMembershipItem(
                    previewTeam.copy(name = "Not reorderable"),
                    isFirstItemInSection = false,
                    isLastItemInSection = true,
                    isPrimary = false,
                    dragHandleModifier = null,
                    onClick = {}
                )
            }
        }
    }
}
