package com.wandr.domain.usecase

import com.wandr.domain.model.Comment
import com.wandr.domain.model.SocialEntityType
import com.wandr.domain.repository.SocialRepository

/** Posts a plain-text comment. */
class AddCommentUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(type: SocialEntityType, entityId: String, userId: String, content: String): Result<Comment> {
        val text = content.trim()
        CommentValidation.error(text)?.let { return Result.failure(it) }
        return repository.addComment(type, entityId, userId, text)
    }
}
