package com.wandr.domain.geo

import com.wandr.domain.model.GpsTrackpoint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrackFilterTest {
    private fun fix(lat: Double, lon: Double, t: Long = 0L, alt: Double = 400.0) = GpsTrackpoint(lat, lon, alt, t)

    // 1 m north is about 0.000009 degrees of latitude.
    private fun meters(north: Double) = 47.0 + north * 0.000009

    @Test
    fun aFixWithAPoorAccuracyIsDropped() {
        assertNull(TrackFilter().accept(fix(47.0, 8.0), accuracyMeters = 80f))
    }

    @Test
    fun theFirstGoodFixIsTheStartOfTheTrack() {
        assertNotNull(TrackFilter().accept(fix(47.0, 8.0), accuracyMeters = 5f))
    }

    @Test
    fun standingStillAddsNoPoints() {
        val filter = TrackFilter()
        var added = 0
        // Jitter of a few meters around one spot.
        repeat(60) { i ->
            val jitterNorth = 2.0 * sin(i * 1.7)
            val jitterEast = 2.0 * cos(i * 2.3) * 0.000013
            if (filter.accept(fix(meters(jitterNorth), 8.0 + jitterEast, t = i * 3000L), accuracyMeters = 6f) != null) added++
        }
        assertTrue(added <= 2, "stood still, but $added points were added")
    }

    @Test
    fun aNoisyWalkIsStraighterAndShorterThanTheRawFixes() {
        // Walking 1.3 m/s north in a straight line, fix every 3 s, with a zig-zag error of +-6 m to the east.
        val filter = TrackFilter()
        val raw = mutableListOf<GpsTrackpoint>()
        val smoothed = mutableListOf<GpsTrackpoint>()
        repeat(100) { i ->
            val zigzag = if (i % 2 == 0) 6.0 else -6.0
            val p = fix(meters(i * 3.9), 8.0 + zigzag * 0.000013, t = i * 3000L)
            raw += p
            filter.accept(p, accuracyMeters = 8f)?.let(smoothed::add)
        }
        fun length(points: List<GpsTrackpoint>) =
            points.zipWithNext().sumOf { (a, b) -> GeoMath.distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude) }
        val trueLength = 99 * 3.9
        assertTrue(length(raw) > trueLength * 1.5, "the raw track should be much longer than the truth because of the zig-zag")
        assertTrue(length(smoothed) < trueLength * 1.15, "smoothed ${length(smoothed)} m vs. true $trueLength m")
        assertTrue(length(smoothed) > trueLength * 0.8, "smoothing must not eat the walk: ${length(smoothed)} m")
    }

    @Test
    fun aPreciseFixCountsMoreThanAnImpreciseOne() {
        val filter = TrackFilter(windowSize = 2, minStepMeters = 0.0)
        filter.accept(fix(meters(0.0), 8.0), accuracyMeters = 25f)
        val result = filter.accept(fix(meters(20.0), 8.0, t = 3000), accuracyMeters = 2f)
        assertNotNull(result)
        // The average sits close to the precise fix at 20 m.
        assertTrue(result.latitude > meters(18.0), "latitude ${result.latitude}")
    }

    @Test
    fun resetForgetsTheHistory() {
        val filter = TrackFilter()
        filter.accept(fix(47.0, 8.0), accuracyMeters = 5f)
        filter.reset()
        // Far away after a pause: not averaged with the old spot, and counted as a new start.
        val result = filter.accept(fix(47.5, 8.5, t = 600_000), accuracyMeters = 5f)
        assertNotNull(result)
        assertEquals(47.5, result.latitude, 1e-9)
    }
}
