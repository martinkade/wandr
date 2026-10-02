package com.wandr.data.fit

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Persistent storage for recorded `.FIT` files, **on this device only**. FIT files are never uploaded to
 * Supabase: they stay on the device that recorded the activity (the activity's metrics are synced, the raw track
 * file is not). Unlike [com.wandr.data.cache.LruFileCache] nothing is evicted; a file lives until its activity
 * is deleted.
 *
 * @param directory app-private folder that holds the files (created on first use)
 */
class FitFileStorage(
    private val directory: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) {

    /** Writes [bytes] for [activityId] (replacing an earlier file) and returns the absolute file path. */
    fun save(activityId: String, bytes: ByteArray): String {
        fileSystem.createDirectories(directory)
        val file = directory / "$activityId.fit"
        fileSystem.write(file) { write(bytes) }
        return file.toString()
    }

    /** Returns the file content, or null if it does not exist (e.g. activity recorded on another device). */
    fun read(path: String): ByteArray? {
        val file = path.toPath().takeIf { isOwnFile(it) && fileSystem.exists(it) } ?: return null
        return fileSystem.read(file) { readByteArray() }
    }

    /** Deletes the file if it exists and belongs to this storage; other paths are ignored. */
    fun delete(path: String) {
        val file = path.toPath().takeIf { isOwnFile(it) } ?: return
        fileSystem.delete(file, mustExist = false)
    }

    /** Only touches files directly inside [directory], never arbitrary paths. */
    private fun isOwnFile(file: Path): Boolean = file.parent == directory && file.name.endsWith(".fit")
}
