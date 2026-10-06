package com.wandr.domain.repository

import com.wandr.domain.model.Comment
import com.wandr.domain.model.SocialCounts
import com.wandr.domain.model.SocialEntityType
import com.wandr.domain.model.SocialSummary

/** Likes, comments and reactions. These live on the server only (they need connectivity, there is no local cache). */
interface SocialRepository {
    suspend fun getSummary(type: SocialEntityType, entityId: String, userId: String): Result<SocialSummary>
    /**
     * Like and comment counts of many items in ONE server call (instead of two queries per feed card); items without
     * likes or comments are in the map with zeros. Whether the user liked an item is decided by the server session.
     */
    suspend fun getCounts(type: SocialEntityType, entityIds: List<String>): Result<Map<String, SocialCounts>>
    suspend fun setLike(type: SocialEntityType, entityId: String, userId: String, liked: Boolean): Result<Unit>

    /** Comments oldest first, with their reactions. */
    suspend fun getComments(type: SocialEntityType, entityId: String, userId: String): Result<List<Comment>>
    suspend fun addComment(type: SocialEntityType, entityId: String, userId: String, content: String): Result<Comment>
    suspend fun updateComment(commentId: String, content: String): Result<Comment>
    suspend fun deleteComment(commentId: String): Result<Unit>
    suspend fun setReaction(commentId: String, userId: String, emoji: String, reacted: Boolean): Result<Unit>
}
