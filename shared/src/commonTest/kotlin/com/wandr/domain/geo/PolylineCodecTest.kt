package com.wandr.domain.geo

import com.wandr.domain.model.GpsTrackpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolylineCodecTest {
    private fun p(lat: Double, lon: Double) = GpsTrackpoint(lat, lon, 0.0, 0L)

    @Test
    fun matchesTheReferenceExampleOfTheAlgorithmDescription() {
        // https://developers.google.com/maps/documentation/utilities/polylinealgorithm
        val points = listOf(p(38.5, -120.2), p(40.7, -120.95), p(43.252, -126.453))
        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", PolylineCodec.encode(points))
    }

    @Test
    fun decodesTheReferenceExample() {
        val decoded = PolylineCodec.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, decoded.size)
        assertEquals(38.5, decoded[0].latitude, 1e-9)
        assertEquals(-120.2, decoded[0].longitude, 1e-9)
        assertEquals(43.252, decoded[2].latitude, 1e-9)
        assertEquals(-126.453, decoded[2].longitude, 1e-9)
    }

    @Test
    fun roundTripKeepsCoordinatesWithinOneMeter() {
        val points = (0..200).map { p(47.3769 + it * 0.00017, 8.5417 - it * 0.00031) }
        val decoded = PolylineCodec.decode(PolylineCodec.encode(points))
        assertEquals(points.size, decoded.size)
        points.zip(decoded).forEach { (a, b) ->
            assertEquals(a.latitude, b.latitude, 1e-5)
            assertEquals(a.longitude, b.longitude, 1e-5)
        }
    }

    @Test
    fun emptyAndTruncatedInput() {
        assertEquals("", PolylineCodec.encode(emptyList()))
        assertTrue(PolylineCodec.decode("").isEmpty())
        assertTrue(PolylineCodec.decode("_p~iF~ps|U_ulL").size == 1) // the incomplete second point is dropped
    }
}
