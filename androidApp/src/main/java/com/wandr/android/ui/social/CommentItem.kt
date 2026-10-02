package com.wandr.android.ui.social

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.res.stringResource
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
    val locale = LocalConfiguration.current.locales[0]
    val author = comment.authorName.ifBlank { stringResource(R.string.social_unknown_author) }

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AvatarImage(avatarUrl = comment.authorAvatarUrl, displayName = author, size = 36.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(author, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = AppDateFormatter.formatDateTime(comment.createdAt, locale = locale) +
                        if (comment.isEdited) " · ${stringResource(R.string.social_edited)}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isEditing) {
                OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth())
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
                Text(comment.content, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                comment.reactions.forEach { reaction ->
                    FilterChip(
                        selected = reaction.reactedByMe,
                        onClick = { onToggleReaction(reaction.emoji) },
                        label = { Text("${reaction.emoji} ${reaction.count}") }
                    )
                }
                androidx.compose.foundation.layout.Box {
                    AssistChip(
                        onClick = { menuOpen = true },
                        label = { Text("+", modifier = Modifier.size(width = 16.dp, height = 20.dp)) },
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

            if (!isEditing && (canEdit || canDelete)) {
                Row {
                    if (canEdit) TextButton(onClick = { isEditing = true }) { Text(stringResource(R.string.social_edit)) }
                    if (canDelete) TextButton(onClick = onDelete) { Text(stringResource(R.string.social_delete)) }
                }
            }
        }
    }
}

private val previewComment = Comment(
    id = "c1", entityType = SocialEntityType.ACTIVITY, entityId = "a1", userId = "u2", authorName = "Alex Kim",
    authorAvatarUrl = null, content = "Great pace today! Which route did you take?", createdAt = 1_768_435_200_000L,
    updatedAt = 1_768_435_900_000L, reactions = listOf(ReactionSummary("👍", 2, true), ReactionSummary("🔥", 1, false))
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 500)
@Composable
private fun CommentItemPreview() {
    WandrTheme {
        CommentItem(previewComment, canEdit = true, canDelete = true, onToggleReaction = {}, onEdit = {}, onDelete = {}, modifier = Modifier.padding(16.dp))
    }
}

@Preview(name = "Read only", showBackground = true)
@Composable
private fun CommentItemReadOnlyPreview() {
    WandrTheme {
        CommentItem(previewComment.copy(updatedAt = previewComment.createdAt), canEdit = false, canDelete = false, onToggleReaction = {}, onEdit = {}, onDelete = {}, modifier = Modifier.padding(16.dp))
    }
}
