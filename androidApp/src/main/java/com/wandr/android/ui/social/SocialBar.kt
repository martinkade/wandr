package com.wandr.android.ui.social

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.theme.WandrTheme

/** Like button with its count and a comments button with the number of comments; the comments open in a sheet. */
@Composable
fun SocialBar(
    likeCount: Int,
    likedByMe: Boolean,
    commentCount: Int,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleLike) {
            Icon(
                painter = painterResource(if (likedByMe) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                contentDescription = stringResource(if (likedByMe) R.string.social_unlike else R.string.social_like),
                tint = if (likedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(likeCount.toString(), style = MaterialTheme.typography.bodyMedium)

        IconButton(onClick = onOpenComments, modifier = Modifier.padding(start = 16.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_comment),
                contentDescription = stringResource(R.string.social_comments_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(commentCount.toString(), style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Composable
private fun SocialBarPreview() {
    WandrTheme {
        SocialBar(
            likeCount = 12, likedByMe = true, commentCount = 4,
            onToggleLike = {}, onOpenComments = {}, modifier = Modifier.padding(16.dp)
        )
    }
}
