package com.wandr.wear

import com.wandr.domain.watch.WatchWorkout
import com.wandr.domain.watch.WatchWorkoutCodec
import com.wandr.wear.model.SendState
import com.wandr.wear.sync.OutboxSyncer
import com.wandr.wear.sync.WorkoutOutbox
import com.wandr.wear.sync.WorkoutTransport
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OutboxTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun workout(id: String) = WatchWorkout(id, "hiking", 0, 1000, 500.0)

    private class FakeTransport(var existing: Set<String> = emptySet(), var fail: Boolean = false) : WorkoutTransport {
        val sentIds = mutableListOf<String>()
        override suspend fun sentIds() = existing
        override suspend fun send(id: String, json: String) {
            if (fail) error("offline")
            sentIds += id
            existing = existing + id
        }
    }

    @Test
    fun entriesSurviveRestartAndCanBeRemoved() {
        val dir = tmp.newFolder("outbox")
        val outbox = WorkoutOutbox(dir)
        outbox.add(workout("a-1"))
        outbox.add(workout("b-2"))
        val reloaded = WorkoutOutbox(dir)
        assertEquals(setOf("a-1", "b-2"), reloaded.entries.value.keys)
        assertEquals("a-1", WatchWorkoutCodec.decode(reloaded.entries.value.getValue("a-1"))?.id)
        reloaded.remove("a-1")
        assertFalse(WorkoutOutbox(dir).contains("a-1"))
    }

    @Test
    fun corruptFilesAreDropped() {
        val dir = tmp.newFolder("outbox")
        dir.resolve("x.json").writeText("garbage")
        assertTrue(WorkoutOutbox(dir).entries.value.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsafeIdsAreRejected() {
        WorkoutOutbox(tmp.newFolder("outbox")).add(workout("../evil"))
    }

    @Test
    fun syncSendsOnlyEntriesWithoutDataItem() = runTest {
        val outbox = WorkoutOutbox(tmp.newFolder("outbox"))
        outbox.add(workout("a"))
        outbox.add(workout("b"))
        val transport = FakeTransport(existing = setOf("a"))
        val syncer = OutboxSyncer(outbox, transport)
        assertEquals(1, syncer.syncAll())
        assertEquals(listOf("b"), transport.sentIds)
        assertEquals(setOf("a", "b"), syncer.sent.value)
    }

    @Test
    fun failedSendStaysPendingAndIsRetried() = runTest {
        val outbox = WorkoutOutbox(tmp.newFolder("outbox"))
        outbox.add(workout("a"))
        val transport = FakeTransport(fail = true)
        val syncer = OutboxSyncer(outbox, transport)
        assertFalse(syncer.send("a"))
        assertEquals(SendState.PENDING, syncer.stateOf("a", outbox.entries.value, syncer.sent.value))
        transport.fail = false
        assertEquals(1, syncer.syncAll())
        assertEquals(SendState.SENT, syncer.stateOf("a", outbox.entries.value, syncer.sent.value))
    }

    @Test
    fun acknowledgementRemovesEntry() = runTest {
        val outbox = WorkoutOutbox(tmp.newFolder("outbox"))
        outbox.add(workout("a"))
        val syncer = OutboxSyncer(outbox, FakeTransport())
        syncer.send("a")
        syncer.onAcknowledged("a")
        assertFalse(outbox.contains("a"))
        assertEquals(SendState.DELIVERED, syncer.stateOf("a", outbox.entries.value, syncer.sent.value))
        assertTrue(syncer.sent.value.isEmpty())
    }
}
