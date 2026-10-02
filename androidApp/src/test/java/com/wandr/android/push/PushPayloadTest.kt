package com.wandr.android.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushPayloadTest {
    @Test
    fun mapsBodyKeys() {
        assertEquals(PushBodyKind.LikeActivity, PushPayload.bodyKind("push.like.activity"))
        assertEquals(PushBodyKind.CommentChallenge, PushPayload.bodyKind("push.comment.challenge"))
        assertEquals(PushBodyKind.ReactionComment, PushPayload.bodyKind("push.reaction.comment"))
        assertNull(PushPayload.bodyKind("push.unknown"))
        assertNull(PushPayload.bodyKind(null))
    }

    @Test
    fun usesLocalizedKindWithActor() {
        val c = PushPayload.content(
            mapOf("body_key" to "push.comment.activity", "actor_name" to "Anna", "preview" to "Nice"), "WANDR", "Anna commented"
        )
        assertEquals(PushBodyKind.CommentActivity, c.kind)
        assertEquals("Anna", c.actorName)
        assertEquals("Nice", c.preview)
    }

    @Test
    fun fallsBackWithoutActorOrKey() {
        assertNull(PushPayload.content(mapOf("body_key" to "push.like.activity"), "T", "B").kind)
        val c = PushPayload.content(emptyMap(), "T", "B")
        assertNull(c.kind)
        assertEquals("B", c.fallbackBody)
    }

    @Test
    fun validatesTarget() {
        assertEquals(PushTarget("activity", "1"), PushPayload.target("activity", "1"))
        assertNull(PushPayload.target("activity", ""))
        assertNull(PushPayload.target("user", "1"))
        assertNull(PushPayload.target(null, null))
    }
}
