package com.wandr.android.ui.social

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.AvatarImage
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.util.AppDateFormatter
import com.wandr.domain.model.Comment
import com.wandr.domain.model.ReactionSummary
import com.wandr.domain.model.Reactions
import com.wandr.domain.model.SocialEntityType

/**
 * One comment with its reactions. The author can edit it, the author and the owner of the commented item can delete
 * it ([canEdit] / [canDelete]); anyone can react.
 */
@Composable
fun CommentItem(
    comment: Comment,
    canEdit: Boolean,
    canDelete: Boolean,
    onToggleReaction: (emoji: String) -> Unit,
    onEdit: (newText: String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by rememberSaveable(comment.id) { mutableStateOf(false) }
    var draft by rememberSaveable(comment.id) { mutableStateOf(comment.content) }
    var menuOpen by rememberSaveable(comment.id) { mutableStateOf(false) }
    var overflowOpen by rememberSaveable(comment.id) { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val author = comment.authorName.ifBlank { stringResource(R.string.social_unknown_author) }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AvatarImage(avatarUrl = comment.authorAvatarUrl, displayName = author, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(author)
                    }
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                        append(" · ")
                        append(
                            AppDateFormatter.formatDateTime(
                                epochMillis = comment.createdAt,
                                locale = locale
                            )
                        )
                        if (comment.isEdited) {
                            append(" (${stringResource(R.string.social_edited)})")
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isEditing) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Row {
                    TextButton(onClick = {
                        isEditing = false
                        draft = comment.content
                    }) { Text(stringResource(R.string.cancel_button)) }
                    TextButton(
                        enabled = draft.isNotBlank() && draft.trim() != comment.content,
                        onClick = {
                            onEdit(draft)
                            isEditing = false
                        }
                    ) { Text(stringResource(R.string.social_save)) }
                }
            } else {
                Text(text = comment.content, style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                comment.reactions.forEach { reaction ->
                    FilterChip(
                        selected = reaction.reactedByMe,
                        onClick = { onToggleReaction(reaction.emoji) },
                        label = { Text("${reaction.emoji} ${reaction.count}") }
                    )
                }
                if (!isEditing) {
                    Box {
                        AssistChip(
                            onClick = { menuOpen = true },
                            label = {
                                Text(
                                    "+",
                                    modifier = Modifier.size(width = 16.dp, height = 20.dp)
                                )
                            },
                            modifier = Modifier.padding(0.dp)
                        )
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            Reactions.allowed.forEach { emoji ->
                                DropdownMenuItem(
                                    text = { Text(emoji) },
                                    onClick = {
                                        menuOpen = false
                                        onToggleReaction(emoji)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Edit and delete are in the overflow menu; not while the comment is being edited.
        if (!isEditing && (canEdit || canDelete)) {
            Box {
                IconButton(onClick = { overflowOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.social_more_options)
                    )
                }
                DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                    if (canEdit) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.social_edit)) },
                            onClick = {
                                overflowOpen = false
                                isEditing = true
                            }
                        )
                    }
                    if (canDelete) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.social_delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                overflowOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

private val previewComment = Comment(
    id = "c1",
    entityType = SocialEntityType.ACTIVITY,
    entityId = "a1",
    userId = "u2",
    authorName = "Alex Kim",
    authorAvatarUrl = null,
    content = "Great pace today! Which route did you take?",
    createdAt = 1_768_435_200_000L,
    updatedAt = 1_768_435_900_000L,
    reactions = listOf(ReactionSummary("👍", 2, true), ReactionSummary("🔥", 1, false))
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 500)
@Composable
private fun CommentItemPreview() {
    WandrTheme {
        CommentItem(
            previewComment,
            canEdit = true,
            canDelete = true,
            onToggleReaction = {},
            onEdit = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Read only", showBackground = true)
@Composable
private fun CommentItemReadOnlyPreview() {
    WandrTheme {
        CommentItem(
            previewComment.copy(updatedAt = previewComment.createdAt),
            canEdit = false,
            canDelete = false,
            onToggleReaction = {},
            onEdit = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
