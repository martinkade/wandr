package com.wandr.domain.model

/** What can be liked and commented on (`social_entity_type` on the server). */
enum class SocialEntityType(val wire: String) {
    ACTIVITY("activity"),
    CHALLENGE("challenge");

    companion object {
        fun fromWire(value: String): SocialEntityType = entries.firstOrNull { it.wire == value } ?: ACTIVITY
    }
}

/** Like and comment numbers of one activity/challenge as shown on a feed card. */
data class SocialCounts(val likeCount: Int = 0, val commentCount: Int = 0, val likedByMe: Boolean = false)

data class SocialSummary(val likeCount: Int = 0, val likedByMe: Boolean = false)

/** One emoji on a comment: how many users reacted with it, and whether the signed-in user is one of them. */
data class ReactionSummary(val emoji: String, val count: Int, val reactedByMe: Boolean)

data class Comment(
    val id: String,
    val entityType: SocialEntityType,
    val entityId: String,
    val userId: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val content: String,
    /** Epoch milliseconds. */
    val createdAt: Long,
    val updatedAt: Long,
    val reactions: List<ReactionSummary> = emptyList()
) {
    val isEdited: Boolean get() = updatedAt - createdAt > EDIT_TOLERANCE_MILLIS

    private companion object {
        const val EDIT_TOLERANCE_MILLIS = 1_000L
    }
}

/** Emojis offered as reactions on a comment. */
object Reactions {
    val allowed = listOf("👍", "❤️", "😂", "🔥", "👏")
}

enum class NotificationType(val wire: String) {
    LIKE("like"),
    COMMENT("comment"),
    REACTION("reaction"),
    INVITE("invite"),
    UNKNOWN("");

    companion object {
        fun fromWire(value: String): NotificationType = entries.firstOrNull { it.wire == value && it != UNKNOWN } ?: UNKNOWN
    }
}

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val actorUserId: String,
    val actorName: String,
    val actorAvatarUrl: String?,
    val entityType: SocialEntityType,
    val entityId: String,
    val commentId: String?,
    /** First characters of the comment (type comment). */
    val preview: String?,
    /** The reaction (type reaction). */
    val emoji: String?,
    val isRead: Boolean,
    /** Epoch milliseconds. */
    val createdAt: Long
)
