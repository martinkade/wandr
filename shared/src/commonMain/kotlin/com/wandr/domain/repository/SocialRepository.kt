package com.wandr.domain.repository

import com.wandr.domain.model.Comment
import com.wandr.domain.model.SocialEntityType
import com.wandr.domain.model.SocialSummary

/** Likes, comments and reactions. These live on the server only (they need connectivity, there is no local cache). */
interface SocialRepository {
    suspend fun getSummary(type: SocialEntityType, entityId: String, userId: String): Result<SocialSummary>
    suspend fun setLike(type: SocialEntityType, entityId: String, userId: String, liked: Boolean): Result<Unit>

    /** Comments oldest first, with their reactions. */
    suspend fun getComments(type: SocialEntityType, entityId: String, userId: String): Result<List<Comment>>
    suspend fun addComment(type: SocialEntityType, entityId: String, userId: String, content: String): Result<Comment>
    suspend fun updateComment(commentId: String, content: String): Result<Comment>
    suspend fun deleteComment(commentId: String): Result<Unit>
    suspend fun setReaction(commentId: String, userId: String, emoji: String, reacted: Boolean): Result<Unit>
}
