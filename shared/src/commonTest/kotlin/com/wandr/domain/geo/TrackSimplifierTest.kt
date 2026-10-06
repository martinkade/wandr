package com.wandr.domain.geo

import com.wandr.domain.model.GpsTrackpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackSimplifierTest {
    private fun p(lat: Double, lon: Double) = GpsTrackpoint(lat, lon, 0.0, 0L)

    @Test
    fun aStraightLineKeepsOnlyItsEnds() {
        val line = (0..100).map { p(47.0 + it * 0.0001, 8.0) }
        val simplified = TrackSimplifier.simplify(line)
        assertEquals(listOf(line.first(), line.last()), simplified)
    }

    @Test
    fun aCornerIsKept() {
        // 500 m north, then 500 m east
        val route = (0..50).map { p(47.0 + it * 0.00009, 8.0) } + (1..50).map { p(47.0 + 50 * 0.00009, 8.0 + it * 0.00013) }
        val simplified = TrackSimplifier.simplify(route)
        assertEquals(3, simplified.size)
        assertEquals(route[50], simplified[1])
    }

    @Test
    fun longNoisyTracksAreCappedAtMaxPoints() {
        val track = (0..5000).map { p(47.0 + it * 0.00002 + (it % 7) * 0.00002, 8.0 + it * 0.00003 - (it % 5) * 0.00002) }
        val simplified = TrackSimplifier.simplify(track, maxPoints = 300)
        assertTrue(simplified.size <= 300, "got ${simplified.size}")
        assertEquals(track.first(), simplified.first())
        assertEquals(track.last(), simplified.last())
    }

    @Test
    fun tinyTracksAreReturnedAsTheyAre() {
        val two = listOf(p(1.0, 1.0), p(2.0, 2.0))
        assertEquals(two, TrackSimplifier.simplify(two))
    }
}
