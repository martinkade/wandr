package com.wandr.wear.sync

import com.wandr.wear.model.SendState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Sends outbox entries through a [WorkoutTransport] and removes them when the phone acknowledges. */
class OutboxSyncer(
    private val outbox: WorkoutOutbox,
    private val transport: WorkoutTransport
) {
    private val mutex = Mutex()
    private val _sent = MutableStateFlow<Set<String>>(emptySet())

    /** Ids that were handed to the transport and are waiting for the phone. */
    val sent: StateFlow<Set<String>> = _sent.asStateFlow()

    fun stateOf(id: String, entries: Map<String, String>, sent: Set<String>): SendState = when {
        id !in entries -> SendState.DELIVERED
        id in sent -> SendState.SENT
        else -> SendState.PENDING
    }

    /**
     * Sends every outbox entry that has no data item yet (e.g. on app start or after a failure).
     * Returns the number of workouts that were sent now.
     */
    suspend fun syncAll(): Int = mutex.withLock {
        val existing = try {
            transport.sentIds()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptySet() // unknown: send again, the put is idempotent
        }
        _sent.value = _sent.value + existing.filter { outbox.contains(it) }
        var count = 0
        for ((id, json) in outbox.entries.value) {
            if (id in existing) continue
            if (sendLocked(id, json)) count++
        }
        count
    }

    /** Sends one entry; false if it failed (it stays pending and is retried by [syncAll]). */
    suspend fun send(id: String): Boolean = mutex.withLock {
        val json = outbox.entries.value[id] ?: return@withLock false
        sendLocked(id, json)
    }

    /** The phone imported (or discarded) the workout: the data item was deleted. */
    fun onAcknowledged(id: String) {
        outbox.remove(id)
        _sent.value = _sent.value - id
    }

    private suspend fun sendLocked(id: String, json: String): Boolean = try {
        transport.send(id, json)
        _sent.value = _sent.value + id
        true
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }
}
