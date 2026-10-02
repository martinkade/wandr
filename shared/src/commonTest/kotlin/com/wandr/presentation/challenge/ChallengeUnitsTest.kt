package com.wandr.presentation.challenge

import com.wandr.domain.model.ChallengeType
import kotlin.test.Test
import kotlin.test.assertEquals

class ChallengeUnitsTest {
    @Test
    fun distanceIsShownInKilometers() {
        assertEquals(100.0, ChallengeUnits.toDisplay(ChallengeType.DISTANCE, 100_000.0))
        assertEquals(42_195.0, ChallengeUnits.toBase(ChallengeType.DISTANCE, 42.195))
    }

    @Test
    fun elevationStaysInMeters() {
        assertEquals(5000.0, ChallengeUnits.toDisplay(ChallengeType.ELEVATION, 5000.0))
        assertEquals(5000.0, ChallengeUnits.toBase(ChallengeType.ELEVATION, 5000.0))
    }

    @Test
    fun timeIsShownInHours() {
        assertEquals(10.0, ChallengeUnits.toDisplay(ChallengeType.TIME, 36_000.0))
        assertEquals(5400.0, ChallengeUnits.toBase(ChallengeType.TIME, 1.5))
    }

    @Test
    fun roundTripKeepsValue() {
        for (type in ChallengeType.entries) {
            assertEquals(1234.5, ChallengeUnits.toBase(type, ChallengeUnits.toDisplay(type, 1234.5)), 1e-9)
        }
    }
}
