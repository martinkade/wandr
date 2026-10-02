package com.wandr.wear.sync

import com.wandr.domain.watch.WatchWorkout
import com.wandr.domain.watch.WatchWorkoutCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Finished workouts that the phone has not acknowledged yet, one `<id>.json` file each in [dir].
 * The JSON is the [WatchWorkoutCodec] wire format, so it can be sent as is.
 */
class WorkoutOutbox(private val dir: File) {
    private val _entries = MutableStateFlow(load())

    /** Workout id to JSON, in insertion order (oldest first). */
    val entries: StateFlow<Map<String, String>> = _entries.asStateFlow()

    @Synchronized
    fun add(workout: WatchWorkout) {
        require(isSafeId(workout.id)) { "Invalid workout id" }
        val json = WatchWorkoutCodec.encode(workout)
        dir.mkdirs()
        val tmp = File(dir, "${workout.id}.tmp")
        tmp.writeText(json)
        java.nio.file.Files.move(tmp.toPath(), file(workout.id).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        _entries.value = _entries.value + (workout.id to json)
    }

    @Synchronized
    fun remove(id: String) {
        if (!isSafeId(id)) return
        file(id).delete()
        if (id in _entries.value) _entries.value = _entries.value - id
    }

    fun contains(id: String): Boolean = id in _entries.value

    private fun file(id: String) = File(dir, "$id.json")

    private fun load(): Map<String, String> {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return emptyMap()
        return files.sortedBy { it.lastModified() }.mapNotNull { f ->
            val json = runCatching { f.readText() }.getOrNull()
            val workout = json?.let(WatchWorkoutCodec::decode)
            if (workout == null) {
                f.delete() // corrupt entry, nothing to recover
                null
            } else workout.id to json
        }.toMap()
    }

    private fun isSafeId(id: String) = id.isNotBlank() && id.all { it.isLetterOrDigit() || it == '-' }
}
