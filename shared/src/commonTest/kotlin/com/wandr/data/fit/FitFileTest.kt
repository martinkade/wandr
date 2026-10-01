package com.wandr.data.fit

import com.wandr.domain.model.GpsTrackpoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FitFileTest {

    @Test
    fun testEncodeAndDecodeTrackpoints() {
        val inputTrackpoints = listOf(
            GpsTrackpoint(latitude = 47.3769, longitude = 8.5417, altitudeMeters = 408.0, timestamp = 1700000000000L, speedMetersPerSecond = 2.5f),
            GpsTrackpoint(latitude = 47.3775, longitude = 8.5425, altitudeMeters = 412.0, timestamp = 1700000010000L, speedMetersPerSecond = 3.0f)
        )

        val fitBytes = FitFileEncoder.encode(
            activityType = "hiking",
            startTimeMs = 1700000000000L,
            endTimeMs = 1700000010000L,
            distanceMeters = 150.0,
            elevationGainMeters = 4.0,
            trackpoints = inputTrackpoints
        )

        assertTrue(fitBytes.isNotEmpty())
        assertEquals(14, fitBytes[0].toInt())

        val decodedTrackpoints = FitFileDecoder.decodeTrackpoints(fitBytes)
        println("DECODED TRACKPOINTS: $decodedTrackpoints")
        assertEquals(2, decodedTrackpoints.size)
        assertTrue(abs(47.3769 - decodedTrackpoints[0].latitude) < 0.001)
        assertTrue(abs(8.5417 - decodedTrackpoints[0].longitude) < 0.001)
        assertTrue(abs(408.0 - decodedTrackpoints[0].altitudeMeters) < 1.0)
    }
}
