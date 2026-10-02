package com.wandr.data.repository

import com.wandr.data.remote.COMMENT_COLUMNS
import com.wandr.data.remote.CommentDto
import com.wandr.data.remote.LikeDto
import com.wandr.data.remote.ReactionDto
import com.wandr.domain.model.Comment
import com.wandr.domain.model.SocialEntityType
import com.wandr.domain.model.SocialSummary
import com.wandr.domain.repository.SocialRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

class SocialRepositoryImpl(private val supabase: SupabaseClient) : SocialRepository {

    override suspend fun getSummary(type: SocialEntityType, entityId: String, userId: String): Result<SocialSummary> = runCatching {
        val likes = supabase.postgrest.from("likes").select(Columns.list("user_id")) {
            filter {
                eq("entity_type", type.wire)
                eq("entity_id", entityId)
            }
        }.decodeList<LikeDto>()
        SocialSummary(likeCount = likes.size, likedByMe = likes.any { it.userId == userId })
    }

    override suspend fun setLike(type: SocialEntityType, entityId: String, userId: String, liked: Boolean): Result<Unit> = runCatching {
        if (liked) {
            supabase.postgrest.from("likes").insert(buildJsonObject {
                put("entity_type", type.wire)
                put("entity_id", entityId)
                put("user_id", userId)
            })
        } else {
            supabase.postgrest.from("likes").delete {
                filter {
                    eq("entity_type", type.wire)
                    eq("entity_id", entityId)
                    eq("user_id", userId)
                }
            }
        }
    }

    override suspend fun getComments(type: SocialEntityType, entityId: String, userId: String): Result<List<Comment>> = runCatching {
        val comments = supabase.postgrest.from("comments").select(Columns.raw(COMMENT_COLUMNS)) {
            filter {
                eq("entity_type", type.wire)
                eq("entity_id", entityId)
            }
            order("created_at", Order.ASCENDING)
        }.decodeList<CommentDto>()

        val reactions = if (comments.isEmpty()) emptyList() else {
            supabase.postgrest.from("comment_reactions").select {
                filter { isIn("comment_id", comments.map { it.id }) }
            }.decodeList<ReactionDto>()
        }.groupBy { it.commentId }

        comments.map { it.toDomain(reactions[it.id].orEmpty(), userId) }
    }

    override suspend fun addComment(type: SocialEntityType, entityId: String, userId: String, content: String): Result<Comment> = runCatching {
        supabase.postgrest.from("comments").insert(buildJsonObject {
            put("entity_type", type.wire)
            put("entity_id", entityId)
            put("user_id", userId)
            put("content", content)
        }) { select(Columns.raw(COMMENT_COLUMNS)) }.decodeSingle<CommentDto>().toDomain(currentUserId = userId)
    }

    override suspend fun updateComment(commentId: String, content: String): Result<Comment> = runCatching {
        supabase.postgrest.from("comments").update(buildJsonObject {
            put("content", content)
            put("updated_at", Clock.System.now().toString())
        }) {
            filter { eq("id", commentId) }
            select(Columns.raw(COMMENT_COLUMNS))
        }.decodeSingle<CommentDto>().toDomain()
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> = runCatching {
        supabase.postgrest.from("comments").delete { filter { eq("id", commentId) } }
    }

    override suspend fun setReaction(commentId: String, userId: String, emoji: String, reacted: Boolean): Result<Unit> = runCatching {
        if (reacted) {
            supabase.postgrest.from("comment_reactions").insert(buildJsonObject {
                put("comment_id", commentId)
                put("user_id", userId)
                put("emoji", emoji)
            })
        } else {
            supabase.postgrest.from("comment_reactions").delete {
                filter {
                    eq("comment_id", commentId)
                    eq("user_id", userId)
                    eq("emoji", emoji)
                }
            }
        }
    }
}
