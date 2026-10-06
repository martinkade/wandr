package com.wandr.android.ui.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityFormatTest {
    @Test
    fun durationBelowAndAboveAnHour() {
        assertEquals("51:07", ActivityFormat.duration(3067.0))
        assertEquals("1:05:07", ActivityFormat.duration(3907.0))
        assertEquals("0:00", ActivityFormat.duration(-5.0))
    }

    @Test
    fun paceInMinutesPerKilometer() {
        assertEquals("5:35", ActivityFormat.pace(9_160.0, 3_067.0))
        assertNull(ActivityFormat.pace(10.0, 600.0))
        assertNull(ActivityFormat.pace(5_000.0, 0.0))
    }

    @Test
    fun speedInKilometersPerHour() {
        assertEquals("30.0", ActivityFormat.speedKmh(15_000.0, 1_800.0))
        assertNull(ActivityFormat.speedKmh(0.0, 100.0))
    }

    @Test
    fun distanceKeepsOneDecimalFromHundredKilometers() {
        assertEquals("9.16", ActivityFormat.distanceKm(9_160.0))
        assertEquals("120.5", ActivityFormat.distanceKm(120_500.0))
    }
}
