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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.AvatarImage
import com.wandr.android.ui.common.sectionItemShape
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.ChallengeType
import com.wandr.domain.model.LeaderboardEntry

/**
 * One member in the ranking of a challenge: rank, avatar, name and progress. A card of a list of connected cards
 * ([isFirstItemInSection] / [isLastItemInSection] round the ends); the signed-in user's row is highlighted.
 */
@Composable
fun LeaderboardRow(
    entry: LeaderboardEntry,
    type: ChallengeType,
    isMe: Boolean,
    isFirstItemInSection: Boolean,
    isLastItemInSection: Boolean,
    modifier: Modifier = Modifier
) {
    val name = entry.displayName.ifBlank { stringResource(R.string.social_unknown_author) }
    val itemShape = sectionItemShape(isFirstItemInSection, isLastItemInSection)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isMe) MaterialTheme.colorScheme.primaryContainer else CardDefaults.cardColors().containerColor,
                shape = itemShape
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "#${entry.rank}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        AvatarImage(avatarUrl = entry.avatarUrl, displayName = name, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isMe) "$name · ${stringResource(R.string.challenge_ranking_you)}" else name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1.0f)
                )
                Text(
                    challengeValueText(type, entry.progressValue),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { (entry.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .weight(1.0f)
                )
                Text(
                    "${entry.progressPercentage.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val previewEntries = listOf(
    LeaderboardEntry(1, "u2", "Alex Kim", null, 82_000.0, 82.0, false),
    LeaderboardEntry(2, "u1", "Martin Kade", null, 75_000.0, 75.0, false),
    LeaderboardEntry(2, "u3", "Sam Lee", null, 75_000.0, 75.0, false),
    LeaderboardEntry(4, "u4", "", null, 100_000.0, 100.0, true)
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 900)
@Composable
private fun LeaderboardRowPreview() {
    WandrTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            previewEntries.forEachIndexed { index, entry ->
                LeaderboardRow(
                    entry,
                    ChallengeType.DISTANCE,
                    isMe = entry.userId == "u1",
                    isFirstItemInSection = index == 0,
                    isLastItemInSection = index == previewEntries.lastIndex
                )
            }
        }
    }
}
