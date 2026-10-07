package com.wandr.android.ui.challenge

import org.junit.Assert.assertEquals
import org.junit.Test

class RemainingTimeTest {
    private val hour = 3_600_000L
    private val day = 24 * hour

    @Test
    fun fromADayOnItCountsDaysRoundedUp() {
        assertEquals(RemainingTime(1, true), RemainingTime.of(day))
        assertEquals(
            RemainingTime(3, true),
            RemainingTime.of(2 * day + 1)
        ) // 2 days and a bit is "in 3 days"
        assertEquals(RemainingTime(7, true), RemainingTime.of(7 * day))
    }

    @Test
    fun belowADayItCountsHoursRoundedUpAtLeastOne() {
        assertEquals(RemainingTime(5, false), RemainingTime.of(4 * hour + 1))
        assertEquals(RemainingTime(23, false), RemainingTime.of(22 * hour + 1))
        assertEquals(RemainingTime(1, true), RemainingTime.of(day - 1)) // not "24 hours"
        assertEquals(RemainingTime(1, false), RemainingTime.of(1))
        assertEquals(RemainingTime(1, false), RemainingTime.of(0))
    }

    @Test
    fun aTimeInThePastIsTreatedAsNoTimeLeft() {
        assertEquals(RemainingTime(1, false), RemainingTime.of(-5 * day))
    }
}
