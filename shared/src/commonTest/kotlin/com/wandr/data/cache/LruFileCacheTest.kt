package com.wandr.data.cache

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LruFileCacheTest {

    @Test
    fun testPutAndGetCacheItem() = runTest {
        val cache = LruFileCache(initialMaxSizeBytes = 1000L)
        val sampleData = "Cover Image Bytes".encodeToByteArray()

        val success = cache.put("cover_1", sampleData)
        assertTrue(success)

        val retrieved = cache.get("cover_1")
        assertNotNull(retrieved)
        assertEquals("Cover Image Bytes", retrieved.decodeToString())
    }

    @Test
    fun testLruEvictionWhenLimitExceeded() = runTest {
        // Cache limit 100 bytes
        val cache = LruFileCache(initialMaxSizeBytes = 100L)
        val data60Bytes = ByteArray(60) { 1 }

        cache.put("file_1", data60Bytes)
        // Access file_1 to make file_2 older
        cache.get("file_1")

        // Put another 60 bytes file -> total 120 bytes > 100 bytes -> evicts file_1
        val data60BytesSecond = ByteArray(60) { 2 }
        cache.put("file_2", data60BytesSecond)

        assertNull(cache.get("file_1")) // file_1 was evicted
        assertNotNull(cache.get("file_2")) // file_2 remains
    }

    @Test
    fun testAdjustableSizeLimit() = runTest {
        val cache = LruFileCache(initialMaxSizeBytes = 200L)
        val data80 = ByteArray(80) { 1 }

        cache.put("item1", data80)
        cache.put("item2", data80)
        assertEquals(2, cache.entryCount)

        // Lower capacity limit down to 100 bytes -> trims item1
        cache.setMaxSizeBytes(100L)
        assertEquals(100L, cache.maxSizeBytes)
        assertEquals(1, cache.entryCount)
    }

    @Test
    fun testJournalLogging() = runTest {
        val cache = LruFileCache(initialMaxSizeBytes = 1000L)
        cache.put("cover_test", ByteArray(50))
        cache.get("cover_test")
        cache.remove("cover_test")

        val journal = cache.getJournal()
        assertEquals(3, journal.size)
        assertEquals(JournalAction.WRITE, journal[0].action)
        assertEquals(JournalAction.READ, journal[1].action)
        assertEquals(JournalAction.REMOVE, journal[2].action)
    }
}
