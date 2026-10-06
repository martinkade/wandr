package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.ChallengeType
import com.wandr.domain.model.LeaderboardEntry

/** One member in the ranking of a challenge: rank, avatar, name and progress. The signed-in user's row is highlighted. */
@Composable
fun LeaderboardRow(
    entry: LeaderboardEntry,
    type: ChallengeType,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val name = entry.displayName.ifBlank { stringResource(R.string.social_unknown_author) }
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        border = if (isMe) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("#${entry.rank}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary)
            AvatarImage(avatarUrl = entry.avatarUrl, displayName = name, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isMe) "$name · ${stringResource(R.string.challenge_ranking_you)}" else name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = { (entry.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(challengeValueText(type, entry.progressValue), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                Text("${entry.progressPercentage.toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Column(Modifier.padding(16.dp)) {
            previewEntries.forEach { LeaderboardRow(it, ChallengeType.DISTANCE, isMe = it.userId == "u1") }
        }
    }
}
