package com.wandr.domain.usecase

import com.wandr.domain.model.Reactions
import com.wandr.domain.repository.SocialRepository

class SetReactionUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(commentId: String, userId: String, emoji: String, reacted: Boolean): Result<Unit> {
        if (emoji !in Reactions.allowed) return Result.failure(IllegalArgumentException("Unsupported reaction"))
        return repository.setReaction(commentId, userId, emoji, reacted)
    }
}
