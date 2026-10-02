package com.wandr.android.push

/** Intent extra keys; they match the FCM data keys, so FCM-displayed (background) notifications carry them too. */
object PushExtras {
    const val ENTITY_TYPE = "entity_type"
    const val ENTITY_ID = "entity_id"
}

/** Localizable text kinds, derived from the `body_key` the server sends. */
enum class PushBodyKind { LikeActivity, LikeChallenge, CommentActivity, CommentChallenge, ReactionComment }

/** What to show for a push data payload: localized via [kind] when known, otherwise the fallback text. */
data class PushContent(
    val kind: PushBodyKind?,
    val actorName: String,
    val preview: String?,
    val emoji: String?,
    val fallbackTitle: String?,
    val fallbackBody: String?
)

/** Pure mapping of the FCM payload; resource lookup happens in [PushNotifier]. */
object PushPayload {
    fun bodyKind(bodyKey: String?): PushBodyKind? = when (bodyKey) {
        "push.like.activity" -> PushBodyKind.LikeActivity
        "push.like.challenge" -> PushBodyKind.LikeChallenge
        "push.comment.activity" -> PushBodyKind.CommentActivity
        "push.comment.challenge" -> PushBodyKind.CommentChallenge
        "push.reaction.comment" -> PushBodyKind.ReactionComment
        else -> null
    }

    fun content(data: Map<String, String>, fallbackTitle: String?, fallbackBody: String?): PushContent {
        val kind = bodyKind(data["body_key"])
        val actor = data["actor_name"]?.takeIf { it.isNotBlank() }
        // Without an actor name the localized sentence would be broken: use the server's fallback text instead.
        return PushContent(
            kind = if (actor != null) kind else null,
            actorName = actor.orEmpty(),
            preview = data["preview"]?.takeIf { it.isNotBlank() },
            emoji = data["emoji"]?.takeIf { it.isNotBlank() },
            fallbackTitle = fallbackTitle,
            fallbackBody = fallbackBody
        )
    }

    fun target(entityType: String?, entityId: String?): PushTarget? {
        if (entityType.isNullOrBlank() || entityId.isNullOrBlank()) return null
        if (entityType != "activity" && entityType != "challenge") return null
        return PushTarget(entityType, entityId)
    }
}
