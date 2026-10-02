package com.wandr.presentation.social

import com.wandr.domain.model.SocialEntityType

sealed interface SocialIntent {
    data class Load(
        val type: SocialEntityType,
        val entityId: String,
        val userId: String,
        val entityOwnerId: String?
    ) : SocialIntent

    object Refresh : SocialIntent
    object ToggleLike : SocialIntent
    data class PostComment(val content: String) : SocialIntent
    data class UpdateComment(val commentId: String, val content: String) : SocialIntent
    data class DeleteComment(val commentId: String) : SocialIntent
    data class ToggleReaction(val commentId: String, val emoji: String) : SocialIntent
    object ClearMessages : SocialIntent
}
