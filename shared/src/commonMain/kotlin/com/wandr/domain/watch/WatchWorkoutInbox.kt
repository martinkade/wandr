package com.wandr.domain.watch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Workouts that arrived from a watch and are not imported yet. The platform receivers (Android Data Layer listener,
 * iOS WatchConnectivity delegate) put them here; `WatchImportViewModel` imports them, which may need a conflict
 * resolution from the user. Held in memory only: the watch side keeps its transfer until [onHandled] acknowledges it.
 */
class WatchWorkoutInbox {
    private val _pending = MutableStateFlow<List<WatchWorkout>>(emptyList())
    val pending: StateFlow<List<WatchWorkout>> = _pending.asStateFlow()

    /** Set by the platform to acknowledge a handled workout to the watch (imported or deliberately discarded). */
    var onHandled: ((workoutId: String) -> Unit)? = null

    /** Adds [workout] unless it is already waiting. */
    fun offer(workout: WatchWorkout) {
        _pending.update { list -> if (list.any { it.id == workout.id }) list else list + workout }
    }

    /** Parses [json] ([WatchWorkoutCodec]) and adds the workout; false if it is invalid. */
    fun offerJson(json: String): Boolean {
        val workout = WatchWorkoutCodec.decode(json) ?: return false
        offer(workout)
        return true
    }

    fun handled(workoutId: String) {
        _pending.update { list -> list.filterNot { it.id == workoutId } }
        onHandled?.invoke(workoutId)
    }
}
