package com.wandr.domain.geo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ContourLinesTest {
    @Test
    fun producesLinesForEveryLevelInsideTheDrawingArea() {
        val set = ContourLines.generate(aspect = 0.5f, levels = 12)
        assertTrue(set.segmentCount > 200, "got ${set.segmentCount}")
        assertEquals(set.segmentCount * 4, set.segments.size)
        assertTrue(set.segments.all { it in -0.001f..1.001f })
        assertTrue(set.levels.all { it in 0 until 12 })
        // Every height level shows up as a line somewhere
        assertEquals(12, set.levels.toSet().size)
    }

    @Test
    fun theSameSeedGivesTheSameTerrainOnEveryPlatform() {
        val a = ContourLines.generate(1.4f, seed = 3)
        val b = ContourLines.generate(1.4f, seed = 3)
        assertTrue(a.segments.contentEquals(b.segments))
        assertNotEquals(a.segmentCount, ContourLines.generate(1.4f, seed = 99).segmentCount)
    }

    @Test
    fun aspectRatioIsClampedSoOddAreasStillWork() {
        assertTrue(ContourLines.generate(0f).segmentCount > 0)
        assertTrue(ContourLines.generate(100f).segmentCount > 0)
    }

    @Test
    fun linesAreShortStepsNotJumpsAcrossTheArea() {
        val set = ContourLines.generate(0.8f, columns = 40)
        for (i in 0 until set.segmentCount) {
            val dx = set.segments[i * 4 + 2] - set.segments[i * 4]
            val dy = set.segments[i * 4 + 3] - set.segments[i * 4 + 1]
            assertTrue(dx * dx + dy * dy < 0.01f, "segment $i is too long")
        }
    }
}
