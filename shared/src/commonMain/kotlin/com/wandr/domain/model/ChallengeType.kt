package com.wandr.domain.model

/** `challenge_type`; the target value is stored in meters (distance, elevation) or seconds (time). */
enum class ChallengeType(val value: String) {
    DISTANCE("distance"),
    ELEVATION("elevation"),
    TIME("time");

    companion object {
        fun fromValue(value: String?): ChallengeType = entries.firstOrNull { it.value == value } ?: DISTANCE
    }
}
