package com.wandr.domain.geo

import com.wandr.domain.model.GpsTrackpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaticMapLayoutTest {
    private fun p(lat: Double, lon: Double) = GpsTrackpoint(lat, lon, 0.0, 0L)

    // About 1.5 km x 1 km around Zurich.
    private val route = listOf(p(47.3700, 8.5300), p(47.3790, 8.5500), p(47.3750, 8.5420))

    @Test
    fun theRouteFitsIntoTheViewportWithPadding() {
        val layout = StaticMapLayout.fit(route, 800.0, 400.0, padding = 32.0)
        route.forEach {
            val (x, y) = layout.project(it.latitude, it.longitude)
            assertTrue(x >= 32.0 - 1e-6 && x <= 800.0 - 32.0 + 1e-6, "x=$x")
            assertTrue(y >= 32.0 - 1e-6 && y <= 400.0 - 32.0 + 1e-6, "y=$y")
        }
    }

    @Test
    fun theZoomIsTheMostDetailedOneThatFits() {
        val layout = StaticMapLayout.fit(route, 800.0, 400.0, padding = 32.0)
        val xs = route.map { layout.project(it.latitude, it.longitude).first }
        val ys = route.map { layout.project(it.latitude, it.longitude).second }
        val extentX = xs.max() - xs.min()
        val extentY = ys.max() - ys.min()
        // One zoom level more doubles the extent in pixels; that must no longer fit.
        assertTrue(
            extentX * 2 > 736.0 || extentY * 2 > 336.0,
            "zoom ${layout.zoom} is not the finest that fits"
        )
    }

    @Test
    fun theRouteIsCentered() {
        val layout = StaticMapLayout.fit(route, 600.0, 600.0)
        val xs = route.map { layout.project(it.latitude, it.longitude).first }
        val ys = route.map { layout.project(it.latitude, it.longitude).second }
        assertEquals(300.0, (xs.min() + xs.max()) / 2, 1e-6)
        assertEquals(300.0, (ys.min() + ys.max()) / 2, 1e-6)
    }

    @Test
    fun tilesCoverTheWholeViewportWithoutGaps() {
        val layout = StaticMapLayout.fit(route, 600.0, 400.0)
        val tiles = layout.tiles
        assertTrue(tiles.isNotEmpty())
        assertTrue(tiles.minOf { it.left } <= 0.0 && tiles.maxOf { it.left } + 256 >= 600.0)
        assertTrue(tiles.minOf { it.top } <= 0.0 && tiles.maxOf { it.top } + 256 >= 400.0)
        assertEquals(tiles.size, tiles.map { it.x to it.y }.toSet().size)
        assertTrue(tiles.all { it.zoom == layout.zoom })
    }

    @Test
    fun knownTileOfZurichAtZoom17() {
        // A one-point route is shown at max zoom, in the tile of the slippy map formula.
        val layout = StaticMapLayout.fit(listOf(p(47.3769, 8.5417)), 256.0, 256.0)
        assertEquals(17, layout.zoom)
        val expectedX = ((8.5417 + 180) / 360 * (1 shl 17)).toInt()
        assertTrue(layout.tiles.any { it.x == expectedX })
    }

    @Test
    fun noPointsGiveAWorldViewAndNoCrash() {
        val layout = StaticMapLayout.fit(emptyList(), 300.0, 300.0)
        assertTrue(layout.tiles.isNotEmpty())
    }

    @Test
    fun tileUrlFillsInZoomColumnAndRow() {
        assertEquals(
            "https://tile.openstreetmap.org/12/2145/1434.png",
            MapTile(2145, 1434, 12, 0.0, 0.0).url
        )
    }
}
