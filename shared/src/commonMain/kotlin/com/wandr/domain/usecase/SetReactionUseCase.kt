package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem
import com.wandr.domain.model.Reactions
import com.wandr.domain.repository.SocialRepository

class SetReactionUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(commentId: String, userId: String, emoji: String, reacted: Boolean): Result<Unit> {
        if (emoji !in Reactions.allowed) return Result.failure(AppError.InvalidInput(InputProblem.UNSUPPORTED_REACTION))
        return repository.setReaction(commentId, userId, emoji, reacted)
    }
}
