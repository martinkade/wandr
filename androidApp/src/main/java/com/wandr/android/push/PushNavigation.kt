package com.wandr.android.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The activity / challenge a tapped push notification wants to open. [entityType] is `activity` or `challenge`. */
data class PushTarget(val entityType: String, val entityId: String)

/** Hand-over from the notification tap (MainActivity intent) to the main screen, which opens the target and consumes it. */
object PushNavigation {
    private val _target = MutableStateFlow<PushTarget?>(null)
    val target: StateFlow<PushTarget?> = _target.asStateFlow()

    fun publish(target: PushTarget) {
        _target.value = target
    }

    fun consume() {
        _target.value = null
    }
}
