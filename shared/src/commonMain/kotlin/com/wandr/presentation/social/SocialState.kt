package com.wandr.presentation.social

import com.wandr.domain.error.AppError
import com.wandr.domain.model.Comment
import com.wandr.domain.model.SocialEntityType
import com.wandr.domain.model.SocialSummary

data class SocialState(
    val entityType: SocialEntityType = SocialEntityType.ACTIVITY,
    val entityId: String? = null,
    val currentUserId: String? = null,
    /** The owner of the activity / challenge: may delete any comment on it. */
    val entityOwnerId: String? = null,
    val summary: SocialSummary = SocialSummary(),
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isPosting: Boolean = false,
    val error: AppError? = null
) {
    /** Only the author edits a comment. */
    fun canEdit(comment: Comment): Boolean = comment.userId == currentUserId

    /** The author and the owner of the commented activity/challenge delete comments. */
    fun canDelete(comment: Comment): Boolean = comment.userId == currentUserId || entityOwnerId == currentUserId
}
