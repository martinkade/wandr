package com.wandr.android.ui.social

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.wandr.presentation.social.SocialIntent
import com.wandr.presentation.social.SocialState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** The comments of an activity or challenge in a bottom sheet: the list with reactions and the composer below it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    state: SocialState,
    onIntent: (SocialIntent) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        CommentsSheetContent(state, onIntent, Modifier.fillMaxHeight(SHEET_HEIGHT_FRACTION))
    }
}

@Composable
internal fun CommentsSheetContent(
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

    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Text(
            text = pluralStringResource(
                R.plurals.social_comments_count,
                state.comments.size,
                state.comments.size
            ),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 96.dp)
        ) {
            if (state.comments.isEmpty() && !state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.social_comments_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(state.comments, key = { it.id }) { comment ->
                CommentItem(
                    comment = comment,
                    canEdit = state.canEdit(comment),
                    canDelete = state.canDelete(comment),
                    onToggleReaction = { onIntent(SocialIntent.ToggleReaction(comment.id, it)) },
                    onEdit = { onIntent(SocialIntent.UpdateComment(comment.id, it)) },
                    onDelete = { onIntent(SocialIntent.DeleteComment(comment.id)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 8.dp,
                            end = if (state.canEdit(comment) || state.canDelete(comment)) 0.dp else 8.dp
                        )
                )
            }
        }

        errorText?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        CommentComposer(
            isPosting = state.isPosting,
            onSend = { onIntent(SocialIntent.PostComment(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .safeDrawingPadding()
        )
    }
}

private const val ERROR_VISIBLE_MILLIS = 4_000L
private const val SHEET_HEIGHT_FRACTION = 0.9f

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

@Preview(name = "Light Mode", showBackground = true, heightDp = 700)
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    heightDp = 700
)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 900)
@Composable
private fun CommentsSheetPreview() {
    WandrTheme {
        CommentsSheetContent(
            state = SocialState(
                currentUserId = "me",
                entityOwnerId = "me",
                comments = previewComments
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "Empty with error", showBackground = true, heightDp = 400)
@Composable
private fun CommentsSheetEmptyPreview() {
    WandrTheme {
        CommentsSheetContent(
            state = SocialState(error = AppError.Network()),
            onIntent = {})
    }
}
