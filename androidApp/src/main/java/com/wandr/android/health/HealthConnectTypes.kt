package com.wandr.android.health

import androidx.health.connect.client.records.ExerciseSessionRecord

/** Maps Health Connect exercise types to the activity types of the app; null for types the app does not offer. */
internal object HealthConnectTypes {
    fun activityType(exerciseType: Int): String? = when (exerciseType) {
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "hiking"

        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "running"

        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "cycling"

        else -> null
    }
}
