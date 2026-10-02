package com.wandr.domain.usecase

import com.wandr.domain.repository.SocialRepository

/** Deletes a comment; allowed for its author and for the owner of the commented activity/challenge. */
class DeleteCommentUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(commentId: String) = repository.deleteComment(commentId)
}
