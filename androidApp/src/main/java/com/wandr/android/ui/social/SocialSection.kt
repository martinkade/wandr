package com.wandr.android.ui.social

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.userMessage
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.error.AppError
import com.wandr.domain.model.Comment
import com.wandr.domain.model.SocialEntityType
import com.wandr.domain.model.SocialSummary
import com.wandr.presentation.social.SocialIntent
import com.wandr.presentation.social.SocialState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Like button, comments with reactions and the comment composer of one activity or challenge. */
@Composable
fun SocialSection(
    state: SocialState,
    onIntent: (SocialIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Errors are shown inline for a moment.
    val errorText = state.error?.userMessage()
    LaunchedEffect(state.error) {
        if (state.error != null) {
            delay(ERROR_VISIBLE_MILLIS.milliseconds)
            onIntent(SocialIntent.ClearMessages)
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onIntent(SocialIntent.ToggleLike) }) {
                val liked = state.summary.likedByMe
                Icon(
                    painter = painterResource(if (liked) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                    contentDescription = stringResource(if (liked) R.string.social_unlike else R.string.social_like),
                    tint = if (liked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = pluralStringResource(
                    R.plurals.social_like_count,
                    state.summary.likeCount,
                    state.summary.likeCount
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = "${stringResource(R.string.social_comments_title)} (${state.comments.size})",
            style = MaterialTheme.typography.titleMedium
        )
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

        if (state.comments.isEmpty() && !state.isLoading) {
            Text(
                text = stringResource(R.string.social_comments_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        state.comments.forEach { comment ->
            CommentItem(
                comment = comment,
                canEdit = state.canEdit(comment),
                canDelete = state.canDelete(comment),
                onToggleReaction = { onIntent(SocialIntent.ToggleReaction(comment.id, it)) },
                onEdit = { onIntent(SocialIntent.UpdateComment(comment.id, it)) },
                onDelete = { onIntent(SocialIntent.DeleteComment(comment.id)) }
            )
        }

        errorText?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        CommentComposer(
            isPosting = state.isPosting,
            onSend = { onIntent(SocialIntent.PostComment(it)) })
        Spacer(Modifier.height(8.dp))
    }
}

private const val ERROR_VISIBLE_MILLIS = 4_000L

private val previewComments = listOf(
    Comment(
        "c1",
        SocialEntityType.ACTIVITY,
        "a1",
        "u2",
        "Alex Kim",
        null,
        "Great pace today!",
        1_768_435_200_000L,
        1_768_435_200_000L
    ),
    Comment(
        "c2",
        SocialEntityType.ACTIVITY,
        "a1",
        "me",
        "Me",
        null,
        "Thanks! The river route.",
        1_768_438_800_000L,
        1_768_438_800_000L
    )
)

@Preview(name = "Light Mode", showBackground = true, heightDp = 900)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    heightDp = 900
)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 1400)
@Composable
private fun SocialSectionPreview() {
    WandrTheme {
        SocialSection(
            state = SocialState(
                currentUserId = "me",
                entityOwnerId = "me",
                summary = SocialSummary(3, true),
                comments = previewComments
            ),
            onIntent = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Empty with error", showBackground = true)
@Composable
private fun SocialSectionEmptyPreview() {
    WandrTheme {
        SocialSection(
            state = SocialState(error = AppError.Network()),
            onIntent = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
