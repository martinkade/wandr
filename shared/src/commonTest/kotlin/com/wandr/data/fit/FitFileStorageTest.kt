package com.wandr.data.fit

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FitFileStorageTest {

    private val fileSystem = FakeFileSystem()
    private val directory = "/app/files/fit".toPath()
    private val storage = FitFileStorage(directory, fileSystem)

    @Test
    fun saveCreatesDirectoryAndReturnsReadablePath() {
        val path = storage.save("a1", byteArrayOf(1, 2, 3))

        assertEquals("/app/files/fit/a1.fit", path)
        assertContentEquals(byteArrayOf(1, 2, 3), storage.read(path))
    }

    @Test
    fun saveReplacesAnEarlierFile() {
        val path = storage.save("a1", byteArrayOf(1))
        storage.save("a1", byteArrayOf(9, 9))

        assertContentEquals(byteArrayOf(9, 9), storage.read(path))
    }

    @Test
    fun readReturnsNullForMissingFile() {
        assertNull(storage.read("/app/files/fit/other-device.fit"))
    }

    @Test
    fun deleteRemovesTheFileAndToleratesMissingOnes() {
        val path = storage.save("a1", byteArrayOf(1))
        storage.delete(path)
        assertNull(storage.read(path))

        storage.delete(path) // already gone: no error
    }

    @Test
    fun filesOutsideTheStorageDirectoryAreNeverReadOrDeleted() {
        fileSystem.createDirectories("/etc".toPath())
        fileSystem.write("/etc/secret.fit".toPath()) { writeUtf8("secret") }

        assertNull(storage.read("/etc/secret.fit"))
        storage.delete("/etc/secret.fit")
        assertTrue(fileSystem.exists("/etc/secret.fit".toPath()))

        // Path traversal does not escape the directory either.
        assertNull(storage.read("/app/files/fit/../../../etc/secret.fit"))
    }
}
