package com.wandr.data.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class LruFileCache(
    initialMaxSizeBytes: Long = 50 * 1024 * 1024L // Default 50 MB
) {
    var maxSizeBytes: Long = initialMaxSizeBytes
        private set

    private val mutex = Mutex()
    private val memoryStore = mutableMapOf<String, ByteArray>()
    private val index = mutableMapOf<String, CacheEntry>()
    private val journal = mutableListOf<JournalRecord>()

    val currentSizeBytes: Long
        get() = index.values.sumOf { it.sizeBytes }

    val entryCount: Int
        get() = index.size

    suspend fun setMaxSizeBytes(newLimitBytes: Long) = mutex.withLock {
        require(newLimitBytes > 0) { "Limit must be positive" }
        maxSizeBytes = newLimitBytes
        trimToSize(maxSizeBytes)
    }

    suspend fun get(key: String): ByteArray? = mutex.withLock {
        val entry = index[key] ?: return null
        val now = Clock.System.now().toEpochMilliseconds()
        entry.lastAccessedAt = now
        
        journal.add(
            JournalRecord(
                action = JournalAction.READ,
                key = key,
                timestamp = now,
                sizeBytes = entry.sizeBytes
            )
        )
        return memoryStore[key]
    }

    suspend fun put(key: String, data: ByteArray): Boolean = mutex.withLock {
        val now = Clock.System.now().toEpochMilliseconds()
        val dataSize = data.size.toLong()

        if (dataSize > maxSizeBytes) {
            return false // Entry exceeds total cache capacity
        }

        // Evict LRU entries until there is space for the new entry
        trimToSize(maxSizeBytes - dataSize)

        memoryStore[key] = data
        index[key] = CacheEntry(
            key = key,
            sizeBytes = dataSize,
            lastAccessedAt = now
        )

        journal.add(
            JournalRecord(
                action = JournalAction.WRITE,
                key = key,
                timestamp = now,
                sizeBytes = dataSize
            )
        )
        return true
    }

    suspend fun remove(key: String): Boolean = mutex.withLock {
        val entry = index.remove(key) ?: return false
        memoryStore.remove(key)
        val now = Clock.System.now().toEpochMilliseconds()

        journal.add(
            JournalRecord(
                action = JournalAction.REMOVE,
                key = key,
                timestamp = now,
                sizeBytes = entry.sizeBytes
            )
        )
        return true
    }

    suspend fun clear() = mutex.withLock {
        memoryStore.clear()
        index.clear()
        journal.clear()
    }

    fun getJournal(): List<JournalRecord> {
        return journal.toList()
    }

    private fun trimToSize(targetSizeBytes: Long) {
        val entriesByLRU = index.values.sortedBy { it.lastAccessedAt }
        for (entry in entriesByLRU) {
            if (currentSizeBytes <= targetSizeBytes) break
            index.remove(entry.key)
            memoryStore.remove(entry.key)

            journal.add(
                JournalRecord(
                    action = JournalAction.EVICT,
                    key = entry.key,
                    timestamp = Clock.System.now().toEpochMilliseconds(),
                    sizeBytes = entry.sizeBytes
                )
            )
        }
    }
}
