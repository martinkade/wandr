package com.wandr.data.sync

/** A row of the local cache, as far as reconciling with the server needs it. */
data class CachedRow(
    val id: String,
    /** False for rows with local edits that are not uploaded yet: they are never removed. */
    val isSynced: Boolean,
    /** Where the row lies in the order the server returned the page in (e.g. the start time of an activity). */
    val sortKey: Long = 0L
)

/**
 * Finds the rows of the local cache that no longer exist on the server (deleted on another device, a team left, a
 * challenge withdrawn from the user's sight), so they can be removed from the cache.
 */
object CacheReconciler {
    /**
     * The ids of the local rows to remove: synced rows that the server did not return.
     *
     * @param windowStart set when the server only returned a page of the newest rows: rows older than [windowStart] were
     *   not asked for and are kept. Null means the server returned everything it has.
     */
    fun staleIds(local: List<CachedRow>, remoteIds: Set<String>, windowStart: Long? = null): List<String> =
        local.filter { row ->
            row.isSynced && row.id !in remoteIds && (windowStart == null || row.sortKey >= windowStart)
        }.map { it.id }
}
