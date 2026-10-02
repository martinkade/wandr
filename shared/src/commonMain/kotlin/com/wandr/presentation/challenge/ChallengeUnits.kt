package com.wandr.presentation.challenge

import com.wandr.domain.model.ChallengeType

/**
 * Challenge targets are stored in meters (distance, elevation) or seconds (time). Forms show friendlier units:
 * kilometers, meters and hours. Shared so Android and iOS convert identically.
 */
object ChallengeUnits {
    private fun factor(type: ChallengeType): Double = when (type) {
        ChallengeType.DISTANCE -> 1000.0 // km -> m
        ChallengeType.ELEVATION -> 1.0 // m -> m
        ChallengeType.TIME -> 3600.0 // h -> s
    }

    fun toDisplay(type: ChallengeType, baseValue: Double): Double = baseValue / factor(type)

    fun toBase(type: ChallengeType, displayValue: Double): Double = displayValue * factor(type)
}
