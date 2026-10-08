package com.wandr.data.sync

import kotlin.test.Test
import kotlin.test.assertEquals

class CacheReconcilerTest {
    private fun row(id: String, synced: Boolean = true, key: Long = 0) = CachedRow(id, synced, key)

    @Test
    fun syncedRowsThatTheServerDoesNotHaveAnymoreAreStale() {
        val stale = CacheReconciler.staleIds(listOf(row("a"), row("b"), row("c")), remoteIds = setOf("a", "c"))
        assertEquals(listOf("b"), stale)
    }

    @Test
    fun unsyncedLocalRowsAreNeverRemoved() {
        val stale = CacheReconciler.staleIds(listOf(row("new", synced = false)), remoteIds = emptySet())
        assertEquals(emptyList(), stale)
    }

    @Test
    fun everythingDeletedOnTheServerEmptiesTheCache() {
        assertEquals(listOf("a", "b"), CacheReconciler.staleIds(listOf(row("a"), row("b")), remoteIds = emptySet()))
    }

    @Test
    fun aPageOfTheNewestRowsOnlyJudgesRowsInsideItsWindow() {
        // The server returned the 100 newest; an older local row is simply outside of it, not deleted.
        val local = listOf(row("new-gone", key = 500), row("old", key = 100), row("new-ok", key = 400))
        val stale = CacheReconciler.staleIds(local, remoteIds = setOf("new-ok"), windowStart = 300)
        assertEquals(listOf("new-gone"), stale)
    }

    @Test
    fun theWindowStartItselfCounts() {
        val stale = CacheReconciler.staleIds(listOf(row("edge", key = 300)), remoteIds = emptySet(), windowStart = 300)
        assertEquals(listOf("edge"), stale)
    }
}
