package com.wandr.android.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.AvatarImage
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.util.AppDateFormatter
import com.wandr.domain.geo.PolylineCodec
import com.wandr.domain.model.Activity
import com.wandr.domain.model.SocialCounts

/**
 * A feed card: who recorded the activity and when, its key figures, the route (if the owner shares it) and the number
 * of likes and comments. The numbers come with the whole list in one request, see `ActivityViewModel`.
 *
 * @param isOwn the signed-in user recorded it; they always see the route and are told when others do not
 */
@Composable
fun ActivityCard(
    activity: Activity,
    authorName: String,
    authorAvatarUrl: String?,
    counts: SocialCounts,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOwn: Boolean = false
) {
    val locale = LocalConfiguration.current.locales[0]
    // Decoding a polyline of a few hundred points is cheap, but not worth repeating on every recomposition.
    val route = remember(activity.polyline) { activity.polyline?.let(PolylineCodec::decode).orEmpty() }
    val author = authorName.ifBlank { stringResource(R.string.social_unknown_author) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AvatarImage(avatarUrl = authorAvatarUrl, displayName = author, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = author,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${AppDateFormatter.formatDateTime(activity.startTime, locale = locale)} · ${activityTypeText(activity.activityType)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = activity.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            ActivityStats(activity)

            if (route.size >= 2) {
                TrackMapView(trackpoints = route, modifier = Modifier.fillMaxWidth().height(180.dp))
                if (isOwn && !activity.showMap) {
                    Text(
                        text = stringResource(R.string.activity_map_hidden_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onLikeClick).padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(if (counts.likedByMe) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                        contentDescription = stringResource(if (counts.likedByMe) R.string.social_unlike else R.string.social_like),
                        tint = if (counts.likedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(counts.likeCount.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_comment),
                        contentDescription = stringResource(R.string.social_comments_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(counts.commentCount.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

/** Distance, pace (or speed on a bike), time and elevation gain. */
@Composable
private fun ActivityStats(activity: Activity) {
    val isCycling = activity.activityType == "cycling"
    val pace = if (isCycling) ActivityFormat.speedKmh(activity.distanceMeters, activity.durationSeconds)
    else ActivityFormat.pace(activity.distanceMeters, activity.durationSeconds)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Stat(stringResource(R.string.activity_stat_distance), "${ActivityFormat.distanceKm(activity.distanceMeters)} km", Modifier.weight(1f))
        if (pace != null) {
            Stat(
                label = stringResource(if (isCycling) R.string.activity_stat_speed else R.string.activity_stat_pace),
                value = if (isCycling) "$pace km/h" else "$pace /km",
                modifier = Modifier.weight(1f)
            )
        }
        Stat(stringResource(R.string.activity_stat_time), ActivityFormat.duration(activity.durationSeconds), Modifier.weight(1f))
        if (activity.elevationGainMeters >= 1) {
            Stat(stringResource(R.string.activity_stat_elevation), "${activity.elevationGainMeters.toInt()} m", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}

private val previewActivity = Activity(
    id = "a1", userId = "u1", teamId = "t1", title = "Afternoon run", description = null, activityType = "running",
    distanceMeters = 9_160.0, durationSeconds = 3_067.0, elevationGainMeters = 85.0, fitFilePath = null,
    startTime = 1_768_435_200_000L, endTime = 1_768_438_267_000L, isManualEntry = false, createdAt = 0L, updatedAt = 0L,
    polyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 1000)
@Preview(name = "Tablet", widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun ActivityCardPreview() {
    WandrTheme {
        Box(Modifier.padding(16.dp)) {
            ActivityCard(
                activity = previewActivity, authorName = "Kristian Blummenfelt", authorAvatarUrl = null,
                counts = SocialCounts(likeCount = 1_800, commentCount = 42, likedByMe = true), onClick = {}, onLikeClick = {}
            )
        }
    }
}

@Preview(name = "Cycling, own, map hidden", showBackground = true)
@Preview(name = "No route (manual)", showBackground = true)
@Composable
private fun ActivityCardOwnPreview() {
    WandrTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ActivityCard(
                activity = previewActivity.copy(activityType = "cycling", showMap = false), authorName = "Me", authorAvatarUrl = null,
                counts = SocialCounts(), onClick = {}, onLikeClick = {}, isOwn = true
            )
            ActivityCard(
                activity = previewActivity.copy(isManualEntry = true, polyline = null, elevationGainMeters = 0.0), authorName = "",
                authorAvatarUrl = null, counts = SocialCounts(2, 0, false), onClick = {}, onLikeClick = {}
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}
