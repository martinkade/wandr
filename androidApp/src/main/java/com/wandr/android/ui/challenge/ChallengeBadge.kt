package com.wandr.android.ui.challenge

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.ChallengeType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A hexagon with its corners at the left and right, like the achievement badges of fitness apps. */
internal val HexagonShape = GenericShape { size, _ ->
    val radius = size.width / 2
    val centerY = size.height / 2
    for (i in 0 until 6) {
        val angle = PI / 3 * i
        val x = radius + radius * cos(angle).toFloat()
        val y = centerY + (size.height / 2) * sin(angle).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@DrawableRes
internal fun challengeTypeIcon(type: ChallengeType): Int = when (type) {
    ChallengeType.DISTANCE -> R.drawable.ic_distance
    ChallengeType.ELEVATION -> R.drawable.ic_terrain
    ChallengeType.TIME -> R.drawable.ic_schedule
}

/** Identifies the badge of a challenge for the hero transition between list item and details. */
internal fun challengeBadgeHeroKey(challengeId: String) = "challenge-badge-$challengeId"

/** The emblem of a challenge: a golden hexagon with the symbol of what is measured (distance, elevation, time). */
@Composable
fun ChallengeBadge(type: ChallengeType, modifier: Modifier = Modifier, size: Dp = 88.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(HexagonShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(size - size * 0.14f)
                .clip(HexagonShape)
                .background(MaterialTheme.colorScheme.onPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(challengeTypeIcon(type)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(size * 0.42f)
            )
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun ChallengeBadgePreview() {
    WandrTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChallengeType.entries.forEach { ChallengeBadge(it) }
            ChallengeBadge(ChallengeType.DISTANCE, size = 56.dp)
        }
    }
}
