package com.wandr.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ChallengeDurationTest {
    private val hour = 60 * 60 * 1000L
    private val day = 24 * hour

    @Test
    fun shorterThanADayIsShownInHours() {
        assertEquals(ChallengeDuration(5, DurationUnit.HOURS), challengeDuration(0, 5 * hour))
        assertEquals(ChallengeDuration(23, DurationUnit.HOURS), challengeDuration(0, 23 * hour))
    }

    @Test
    fun veryShortChallengesAreAtLeastOneHour() {
        assertEquals(ChallengeDuration(1, DurationUnit.HOURS), challengeDuration(0, 5 * 60 * 1000L))
        assertEquals(ChallengeDuration(1, DurationUnit.HOURS), challengeDuration(0, 0))
    }

    @Test
    fun aDayOrLongerIsShownInDays() {
        assertEquals(ChallengeDuration(1, DurationUnit.DAYS), challengeDuration(0, day))
        assertEquals(ChallengeDuration(30, DurationUnit.DAYS), challengeDuration(0, 30 * day))
    }

    @Test
    fun daysAreRoundedToTheNearestDay() {
        // Mon 00:00 to Sun 23:59 reads as 7 days, not 6.
        assertEquals(ChallengeDuration(7, DurationUnit.DAYS), challengeDuration(0, 7 * day - 60_000L))
        assertEquals(ChallengeDuration(2, DurationUnit.DAYS), challengeDuration(0, day + 13 * hour))
        assertEquals(ChallengeDuration(1, DurationUnit.DAYS), challengeDuration(0, day + 11 * hour))
    }

    @Test
    fun endBeforeStartDoesNotGoNegative() {
        assertEquals(ChallengeDuration(1, DurationUnit.HOURS), challengeDuration(1000, 0))
    }
}
