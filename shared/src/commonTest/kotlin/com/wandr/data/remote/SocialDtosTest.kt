package com.wandr.data.remote

import com.wandr.domain.model.NotificationType
import com.wandr.domain.model.ReactionSummary
import com.wandr.domain.model.SocialEntityType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SocialDtosTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun commentWithAuthorAndReactionsIsMapped() {
        val dto = json.decodeFromString<CommentDto>(
            """{"id":"c1","entity_type":"activity","entity_id":"a1","user_id":"u1","content":"Hi",
               "created_at":"2026-01-01T10:00:00Z","updated_at":"2026-01-01T10:05:00Z",
               "profiles":{"display_name":"Alex","avatar_url":null}}"""
        )
        val reactions = listOf(ReactionDto("c1", "me", "🔥"), ReactionDto("c1", "x", "🔥"), ReactionDto("c1", "x", "👍"))
        val comment = dto.toDomain(reactions, currentUserId = "me")

        assertEquals("Alex", comment.authorName)
        assertTrue(comment.isEdited)
        assertEquals(SocialEntityType.ACTIVITY, comment.entityType)
        // Sorted like the offered emojis: 👍 before 🔥.
        assertEquals(listOf(ReactionSummary("👍", 1, false), ReactionSummary("🔥", 2, true)), comment.reactions)
    }

    @Test
    fun unchangedCommentIsNotMarkedEdited() {
        val dto = json.decodeFromString<CommentDto>(
            """{"id":"c1","entity_type":"challenge","entity_id":"x","user_id":"u1","content":"Hi",
               "created_at":"2026-01-01T10:00:00.100Z","updated_at":"2026-01-01T10:00:00.400Z"}"""
        )
        assertFalse(dto.toDomain().isEdited)
        assertEquals("", dto.toDomain().authorName)
    }

    @Test
    fun notificationReadsItsPayload() {
        val dto = json.decodeFromString<NotificationDto>(
            """{"id":"n1","actor_user_id":"u2","notification_type":"reaction","target_id":"a1",
               "payload":{"entity_type":"activity","entity_id":"a1","comment_id":"c1","emoji":"👏"},
               "is_read":false,"created_at":"2026-01-01T10:00:00Z","actor":{"display_name":"Sam"}}"""
        )
        val n = dto.toDomain()
        assertEquals(NotificationType.REACTION, n.type)
        assertEquals("Sam", n.actorName)
        assertEquals("c1", n.commentId)
        assertEquals("👏", n.emoji)
        assertEquals(NotificationType.UNKNOWN, NotificationType.fromWire("something-new"))
    }
}
