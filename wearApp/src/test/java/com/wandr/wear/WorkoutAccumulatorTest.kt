package com.wandr.wear

import com.wandr.wear.tracking.WorkoutAccumulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutAccumulatorTest {
    @Test
    fun reportedTotalsWinAndNeverDecrease() {
        val acc = WorkoutAccumulator()
        acc.onDistanceTotal(100.0)
        acc.onDistanceTotal(50.0)
        acc.onElevationGainTotal(12.0)
        assertEquals(100.0, acc.distanceMeters, 0.0)
        assertEquals(12.0, acc.elevationGainMeters, 0.0)
    }

    @Test
    fun heartRateStatsIgnoreInvalidSamples() {
        val acc = WorkoutAccumulator()
        listOf(100.0, 120.0, 0.0, Double.NaN, 300.0, 140.0).forEach(acc::onHeartRate)
        val w = acc.build("id", "running", 0, 1000)
        assertEquals(120, w.averageHeartRate)
        assertEquals(140, w.maxHeartRate)
        assertEquals(140, acc.currentHeartRate)
    }

    @Test
    fun noHeartRateGivesNulls() {
        val w = WorkoutAccumulator().build("id", "hiking", 0, 1000)
        assertNull(w.averageHeartRate)
        assertNull(w.maxHeartRate)
    }

    @Test
    fun locationsAreFilteredAndDeriveDistanceAndElevation() {
        val acc = WorkoutAccumulator()
        assertTrue(acc.onLocation(48.0, 11.0, 500.0, 0))
        assertFalse(acc.onLocation(48.0, 11.0, 500.0, 500)) // too close in time
        assertFalse(acc.onLocation(Double.NaN, 11.0, 500.0, 5_000))
        assertFalse(acc.onLocation(95.0, 11.0, 500.0, 5_000))
        assertTrue(acc.onLocation(48.001, 11.0, 510.0, 10_000))
        assertTrue(acc.onLocation(48.002, 11.0, Double.NaN, 20_000))
        assertEquals(3, acc.trackpoints.size)
        assertEquals(222.4, acc.distanceMeters, 1.0) // 0.002 deg latitude
        assertEquals(10.0, acc.elevationGainMeters, 0.0)
        assertEquals(510.0, acc.trackpoints[2].altitudeMeters, 0.0)
        assertTrue(acc.trackpoints[1].speedMetersPerSecond > 10f)
    }

    @Test
    fun buildProducesValidWorkout() {
        val acc = WorkoutAccumulator()
        acc.onDistanceTotal(1000.0)
        val w = acc.build("abc", "cycling", 2_000, 1_000) // end before start is clamped
        assertEquals("abc", w.id)
        assertEquals("cycling", w.activityType)
        assertEquals(2_000, w.endTime)
        assertEquals(1000.0, w.distanceMeters, 0.0)
    }
}
