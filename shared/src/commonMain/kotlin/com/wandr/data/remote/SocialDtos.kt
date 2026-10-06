package com.wandr.data.remote

import com.wandr.domain.model.AppNotification
import com.wandr.domain.model.Comment
import com.wandr.domain.model.NotificationType
import com.wandr.domain.model.SocialEntityType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant

/** Selects a comment together with its author (the only foreign key from `comments` to `profiles`). */
internal const val COMMENT_COLUMNS = "*, profiles(display_name, avatar_url)"

/** `notifications` has two foreign keys to `profiles`; the hint picks the actor. */
internal const val NOTIFICATION_COLUMNS = "*, actor:profiles!actor_user_id(display_name, avatar_url)"

@Serializable
data class AuthorDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class LikeDto(@SerialName("user_id") val userId: String)

@Serializable
data class ReactionDto(
    @SerialName("comment_id") val commentId: String,
    @SerialName("user_id") val userId: String,
    val emoji: String
)

@Serializable
data class CommentDto(
    val id: String,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val profiles: AuthorDto? = null
) {
    fun toDomain(reactions: List<ReactionDto> = emptyList(), currentUserId: String = ""): Comment = Comment(
        id = id,
        entityType = SocialEntityType.fromWire(entityType),
        entityId = entityId,
        userId = userId,
        authorName = profiles?.displayName.orEmpty(),
        authorAvatarUrl = profiles?.avatarUrl,
        content = content,
        createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
        updatedAt = Instant.parse(updatedAt).toEpochMilliseconds(),
        reactions = summarize(reactions, currentUserId)
    )
}

/** Groups raw reactions by emoji, in the order the emojis are offered. */
internal fun summarize(reactions: List<ReactionDto>, currentUserId: String) =
    reactions.groupBy { it.emoji }
        .map { (emoji, list) -> com.wandr.domain.model.ReactionSummary(emoji, list.size, list.any { it.userId == currentUserId }) }
        .sortedBy { summary -> com.wandr.domain.model.Reactions.allowed.indexOf(summary.emoji).let { if (it < 0) Int.MAX_VALUE else it } }

@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("actor_user_id") val actorUserId: String,
    @SerialName("notification_type") val type: String,
    @SerialName("target_id") val targetId: String,
    val payload: JsonObject = JsonObject(emptyMap()),
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String,
    val actor: AuthorDto? = null
) {
    private fun payloadText(key: String) = payload[key]?.jsonPrimitive?.contentOrNull

    fun toDomain() = AppNotification(
        id = id,
        type = NotificationType.fromWire(type),
        actorUserId = actorUserId,
        actorName = actor?.displayName.orEmpty(),
        actorAvatarUrl = actor?.avatarUrl,
        entityType = SocialEntityType.fromWire(payloadText("entity_type").orEmpty()),
        entityId = payloadText("entity_id") ?: targetId,
        commentId = payloadText("comment_id"),
        preview = payloadText("preview"),
        emoji = payloadText("emoji"),
        isRead = isRead,
        createdAt = Instant.parse(createdAt).toEpochMilliseconds()
    )
}

/** Row of the `social_counts` function. */
@Serializable
data class SocialCountsDto(
    @SerialName("entity_id") val entityId: String,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("liked_by_me") val likedByMe: Boolean = false
) {
    fun toDomain() = com.wandr.domain.model.SocialCounts(likeCount, commentCount, likedByMe)
}
