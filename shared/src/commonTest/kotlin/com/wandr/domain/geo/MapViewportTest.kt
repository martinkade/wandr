package com.wandr.domain.geo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapViewportTest {
    private val zurich = MapViewport(47.3769, 8.5417, zoom = 15.0, width = 400.0, height = 800.0)

    @Test
    fun theCenterIsInTheMiddleOfTheView() {
        val (x, y) = zurich.project(47.3769, 8.5417)
        assertEquals(200.0, x, 1e-6)
        assertEquals(400.0, y, 1e-6)
    }

    @Test
    fun fromScreenIsTheInverseOfProject() {
        val (x, y) = zurich.project(47.38, 8.55)
        val (lat, lon) = zurich.fromScreen(x, y)
        assertEquals(47.38, lat, 1e-7)
        assertEquals(8.55, lon, 1e-7)
    }

    @Test
    fun draggingMovesTheContentWithTheFinger() {
        val before = zurich.project(47.38, 8.55)
        val after = zurich.panned(30.0, -20.0).project(47.38, 8.55)
        assertEquals(before.first + 30.0, after.first, 1e-6)
        assertEquals(before.second - 20.0, after.second, 1e-6)
    }

    @Test
    fun pinchingKeepsTheCoordinateUnderTheFingersInPlace() {
        val (lat, lon) = zurich.fromScreen(300.0, 150.0)
        val zoomed = zurich.zoomedAround(17.0, 300.0, 150.0)
        val (x, y) = zoomed.project(lat, lon)
        assertEquals(300.0, x, 1e-5)
        assertEquals(150.0, y, 1e-5)
        assertEquals(17.0, zoomed.zoom)
    }

    @Test
    fun zoomIsLimited() {
        assertEquals(MapViewport.MAX_ZOOM.toDouble(), zurich.zoomedAround(30.0, 0.0, 0.0).zoom)
        assertEquals(MapViewport.MIN_ZOOM.toDouble(), zurich.zoomedAround(-4.0, 0.0, 0.0).zoom)
    }

    @Test
    fun aFractionalZoomScalesTheTilesOfTheNearestLevel() {
        val v = MapViewport(47.0, 8.0, zoom = 15.5, width = 400.0, height = 400.0)
        // 15.5 rounds to level 16; the tiles are shown at 2^-0.5 of their size.
        assertEquals(16, v.tileZoom)
        assertEquals(256 * 2.0.let { kotlin.math.sqrt(0.5) }, v.tileSizePx, 1e-9)
    }

    @Test
    fun tilesCoverTheWholeViewWithoutGaps() {
        val v = MapViewport(47.0, 8.0, zoom = 14.3, width = 500.0, height = 900.0)
        val tiles = v.tiles
        assertTrue(tiles.minOf { it.left } <= 0.0 && tiles.maxOf { it.left } + v.tileSizePx >= 500.0)
        assertTrue(tiles.minOf { it.top } <= 0.0 && tiles.maxOf { it.top } + v.tileSizePx >= 900.0)
        assertEquals(tiles.size, tiles.map { it.x to it.y }.toSet().size)
    }

    @Test
    fun aSpreadByFactorTwoIsOneZoomLevel() {
        assertEquals(16.0, MapViewport.zoomAfterPinch(15.0, 2.0), 1e-9)
        assertEquals(14.0, MapViewport.zoomAfterPinch(15.0, 0.5), 1e-9)
    }
}
