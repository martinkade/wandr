package com.wandr.domain.usecase

import com.wandr.domain.model.Comment
import com.wandr.domain.repository.SocialRepository

/** Changes the text of a comment; only its author may do that (enforced by the server as well). */
class UpdateCommentUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(commentId: String, content: String): Result<Comment> {
        val text = content.trim()
        CommentValidation.error(text)?.let { return Result.failure(it) }
        return repository.updateComment(commentId, text)
    }
}
