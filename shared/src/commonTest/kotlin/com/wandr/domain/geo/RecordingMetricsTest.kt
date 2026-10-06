package com.wandr.domain.geo

import com.wandr.domain.model.GpsTrackpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RecordingMetricsTest {
    /** One degree of latitude is about 111.2 km, so 0.001 degrees are about 111 m. */
    private fun point(index: Int, secondsApart: Int, latStep: Double = 0.001) =
        GpsTrackpoint(47.0 + index * latStep, 8.0, 400.0, index * secondsApart * 1000L)

    @Test
    fun distanceBetweenTwoCoordinates() {
        // Zurich -> Bern, roughly 95 km as the crow flies
        val d = GeoMath.distanceMeters(47.3769, 8.5417, 46.9480, 7.4474)
        assertEquals(95_000.0, d, 3_000.0)
        assertEquals(0.0, GeoMath.distanceMeters(47.0, 8.0, 47.0, 8.0), 1e-9)
    }

    @Test
    fun paceFromTheLastThirtySeconds() {
        // 111 m every 30 s -> 270 s per km
        val points = (0..6).map { point(it, secondsApart = 30) }
        val pace = assertNotNull(RecordingMetrics.recentPaceSecondsPerKm(points, windowMillis = 60_000))
        assertEquals(270.0, pace, 5.0)
    }

    @Test
    fun noPaceWhileStandingStillOrWithTooFewPoints() {
        val still = (0..5).map { GpsTrackpoint(47.0, 8.0, 400.0, it * 5_000L) }
        assertNull(RecordingMetrics.recentPaceSecondsPerKm(still))
        assertNull(RecordingMetrics.recentPaceSecondsPerKm(listOf(point(0, 5))))
        assertNull(RecordingMetrics.recentPaceSecondsPerKm(emptyList()))
    }

    @Test
    fun onlyThePointsInsideTheWindowCount() {
        // a long standstill, then moving: the old points must not drag the pace down
        val standing = (0..10).map { GpsTrackpoint(47.0, 8.0, 400.0, it * 10_000L) }
        val moving = (1..4).map { GpsTrackpoint(47.0 + it * 0.0005, 8.0, 400.0, 100_000L + it * 10_000L) }
        val pace = assertNotNull(RecordingMetrics.recentPaceSecondsPerKm(standing + moving, windowMillis = 30_000))
        assertEquals(180.0, pace, 10.0) // ~55 m per 10 s
    }

    @Test
    fun elevationGainIgnoresJitterAndDescents() {
        val tracker = ElevationGainTracker(thresholdMeters = 3.0)
        assertEquals(0.0, tracker.add(100.0))
        assertEquals(0.0, tracker.add(101.5)) // noise
        assertEquals(0.0, tracker.add(99.0)) // noise
        assertEquals(6.0, tracker.add(106.0), 1e-9) // real climb, measured against the last accepted altitude
        assertEquals(0.0, tracker.add(100.0)) // descent: no gain, new reference
        assertEquals(5.0, tracker.add(105.0), 1e-9)
        tracker.reset()
        assertEquals(0.0, tracker.add(500.0))
    }
}

class RecordingPolicyTest {
    @Test
    fun fastSportsSampleEverySecondAndHikingEveryThreeSeconds() {
        assertEquals(1_000L, RecordingPolicy.sampleIntervalMillis("cycling"))
        assertEquals(1_000L, RecordingPolicy.sampleIntervalMillis("running"))
        assertEquals(1_000L, RecordingPolicy.sampleIntervalMillis("something-new"))
        assertEquals(3_000L, RecordingPolicy.sampleIntervalMillis("hiking"))
        assertEquals(3_000L, RecordingPolicy.sampleIntervalMillis("walking"))
    }

    @Test
    fun aFixThatArrivesAFewMillisecondsEarlyIsStillKept() {
        assertEquals(2_700L, RecordingPolicy.minGapMillis("hiking"))
        assertEquals(900L, RecordingPolicy.minGapMillis("running"))
    }
}
