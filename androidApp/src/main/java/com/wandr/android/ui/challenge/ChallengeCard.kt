package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeScope
import com.wandr.domain.model.ChallengeStatus
import com.wandr.domain.model.ChallengeType

/**
 * A challenge in the list, built like the details screen: cover (or elevation lines) with the emblem on its edge, then
 * title, description and the key facts. Tapping opens the details; joining and leaving happen there only.
 */
@Composable
fun ChallengeCard(
    challenge: Challenge,
    /** Derived at runtime from the dates, never stored (see `EvaluateChallengeStatusUseCase`). */
    status: ChallengeStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val type = ChallengeType.fromValue(challenge.type)
    val isGroup = ChallengeScope.fromValue(challenge.scope) == ChallengeScope.GROUP
    val remaining = challengeRemainingText(challenge, status)

    Column(
        modifier
            .background(
                color = CardDefaults.cardColors().containerColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth()) {
                ChallengeHero(
                    coverUrl = challenge.coverUrl,
                    seed = challenge.id.hashCode(),
                    height = 128.dp
                )
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(statusLabel(status)).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChallengeBadge(type = type, size = 64.dp)
                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOfNotNull(
                            challengeLengthText(challenge),
                            remaining
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            challenge.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                CardFact(
                    icon = R.drawable.ic_flag,
                    text = stringResource(
                        R.string.challenge_goal_text,
                        stringResource(typeLabel(type)),
                        challengeValueText(type, challenge.targetValue)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                )
                if (isGroup) {
                    CardFact(
                        icon = R.drawable.ic_tab_groups,
                        text = stringResource(R.string.challenge_scope_group),
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFact(icon: Int, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun typeLabel(type: ChallengeType) = when (type) {
    ChallengeType.DISTANCE -> R.string.challenge_type_distance
    ChallengeType.ELEVATION -> R.string.challenge_type_elevation
    ChallengeType.TIME -> R.string.challenge_type_time
}

private fun statusLabel(status: ChallengeStatus) = when (status) {
    ChallengeStatus.DRAFT -> R.string.challenge_status_draft
    ChallengeStatus.PLANNED -> R.string.challenge_status_planned
    ChallengeStatus.ACTIVE -> R.string.challenge_status_active
    ChallengeStatus.COMPLETED -> R.string.challenge_status_completed
    ChallengeStatus.EXPIRED -> R.string.challenge_status_expired
}

private val previewChallenge = Challenge(
    id = "c1",
    title = "30-Day 100km Hike",
    description = "Hike 100km total within 30 days. Any hike counts!",
    coverUrl = null,
    scope = "group",
    type = "distance",
    targetValue = 100_000.0,
    requireAllMembersCompletion = true,
    startDate = 1_768_435_200_000L,
    endDate = 1_771_027_200_000L,
    createdBy = "u1",
    createdAt = 0L,
    updatedAt = 0L
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 900)
@Composable
private fun ChallengeCardPreview() {
    WandrTheme {
        Box(Modifier.padding(16.dp)) {
            ChallengeCard(
                previewChallenge,
                ChallengeStatus.ACTIVE,
                onClick = {})
        }
    }
}

@Preview(name = "Elevation, draft", showBackground = true)
@Preview(
    name = "Time, planned Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun ChallengeCardVariantsPreview() {
    WandrTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ChallengeCard(
                previewChallenge.copy(
                    id = "c2",
                    title = "Team Altitude Climb",
                    description = null,
                    scope = "individual",
                    type = "elevation",
                    targetValue = 5000.0
                ),
                ChallengeStatus.DRAFT, onClick = {}
            )
            ChallengeCard(
                previewChallenge.copy(
                    id = "c3",
                    title = "Marathon Hours",
                    type = "time",
                    targetValue = 36_000.0
                ), ChallengeStatus.PLANNED, onClick = {})
        }
    }
}
