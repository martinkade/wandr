package com.wandr.android.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.activity.components.ActivityOwner
import com.wandr.android.ui.activity.components.ActivityStats
import com.wandr.android.ui.activity.components.ActivityTitle
import com.wandr.android.ui.social.SocialBar
import com.wandr.android.ui.theme.WandrTheme
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
    isOwn: Boolean = false,
    onCommentsClick: () -> Unit = onClick
) {
    // Decoding a polyline of a few hundred points is cheap, but not worth repeating on every recomposition.
    val route = remember(activity.polyline) {
        activity.polyline?.let(PolylineCodec::decode).orEmpty()
    }

    Column(
        modifier
            .background(
                color = CardDefaults.cardColors().containerColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
    ) {
        ActivityOwner(
            author = authorName.ifBlank { stringResource(R.string.social_unknown_author) },
            authorAvatarUrl = authorAvatarUrl,
            activity = activity,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        )

        ActivityTitle(
            activity = activity,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        ActivityStats(
            activity = activity,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        )

        if (route.size >= 2) {
            OsmTrackMap(
                trackpoints = route,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(192.dp)
            )
            if (isOwn && !activity.showMap) {
                Text(
                    text = stringResource(R.string.activity_map_hidden_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SocialBar(
            likeCount = counts.likeCount,
            likedByMe = counts.likedByMe,
            commentCount = counts.commentCount,
            onToggleLike = onLikeClick,
            onOpenComments = onCommentsClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}

private val previewActivity = Activity(
    id = "a1",
    userId = "u1",
    teamId = "t1",
    title = "Afternoon run",
    description = "Cycling, own, map hidden",
    activityType = "running",
    distanceMeters = 9_160.0,
    durationSeconds = 3_067.0,
    elevationGainMeters = 85.0,
    fitFilePath = null,
    startTime = 1_768_435_200_000L,
    endTime = 1_768_438_267_000L,
    isManualEntry = false,
    createdAt = 0L,
    updatedAt = 0L,
    polyline = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 1000)
@Preview(name = "Tablet", widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun ActivityCardPreview() {
    WandrTheme {
        Surface {
            ActivityCard(
                activity = previewActivity,
                authorName = "Kristian Blummenfelt",
                authorAvatarUrl = null,
                counts = SocialCounts(likeCount = 1_800, commentCount = 42, likedByMe = true),
                onClick = {},
                onLikeClick = {}
            )
        }
    }
}

@Preview(name = "Cycling, own, map hidden", showBackground = true)
@Preview(name = "No route (manual)", showBackground = true)
@Composable
private fun ActivityCardOwnPreview() {
    WandrTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityCard(
                    activity = previewActivity.copy(activityType = "cycling", showMap = false),
                    authorName = "Me",
                    authorAvatarUrl = null,
                    counts = SocialCounts(),
                    onClick = {},
                    onLikeClick = {},
                    isOwn = true
                )
                ActivityCard(
                    activity = previewActivity.copy(
                        isManualEntry = true,
                        polyline = null,
                        elevationGainMeters = 0.0
                    ),
                    authorName = "",
                    authorAvatarUrl = null,
                    counts = SocialCounts(2, 0, false),
                    onClick = {},
                    onLikeClick = {}
                )
            }
        }
    }
}
