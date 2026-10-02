package com.wandr.wear.model

enum class WorkoutPhase { IDLE, STARTING, ACTIVE, PAUSED }

/** Snapshot of the running workout, shown by the live screen. */
data class LiveWorkoutState(
    val phase: WorkoutPhase = WorkoutPhase.IDLE,
    val type: WearActivityType = WearActivityType.HIKING,
    val distanceMeters: Double = 0.0,
    val heartRate: Int? = null,
    val elevationGainMeters: Double = 0.0,
    val elapsedMs: Long = 0L
)
