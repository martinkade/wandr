package com.wandr.domain.geo

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The part of a slippy map (OpenStreetMap, Web Mercator) that a view of [width] x [height] pixels shows: centered on a
 * coordinate at a fractional [zoom]. For a map the user can move: [panned] and [zoomedAround] give the next viewport
 * while a finger drags or pinches; [tiles] are the tiles to draw and [project] places coordinates (the route) on them.
 *
 * Tiles come at whole zoom levels; between two of them the tiles are scaled by [scale] (1 to 1.41).
 */
class MapViewport(
    val centerLatitude: Double,
    val centerLongitude: Double,
    val zoom: Double,
    val width: Double,
    val height: Double
) {
    /** The zoom level of the tiles. */
    val tileZoom: Int = zoom.roundToInt().coerceIn(MIN_ZOOM, MAX_ZOOM)

    /** How much the tiles of [tileZoom] are enlarged (or reduced) on the screen. */
    val scale: Double = 2.0.pow(zoom - tileZoom)

    /** Edge of a tile on the screen, in pixels. */
    val tileSizePx: Double = WebMercator.TILE_SIZE * scale

    private val centerX = WebMercator.worldX(centerLongitude, tileZoom)
    private val centerY = WebMercator.worldY(centerLatitude, tileZoom)

    /** Position of a coordinate on the view, in pixels (may lie outside of it). */
    fun project(latitude: Double, longitude: Double): Pair<Double, Double> = Pair(
        (WebMercator.worldX(longitude, tileZoom) - centerX) * scale + width / 2,
        (WebMercator.worldY(latitude, tileZoom) - centerY) * scale + height / 2
    )

    /** The coordinate under a point of the view: (latitude, longitude). */
    fun fromScreen(x: Double, y: Double): Pair<Double, Double> {
        val worldX = centerX + (x - width / 2) / scale
        val worldY = centerY + (y - height / 2) / scale
        return Pair(WebMercator.latitude(worldY, tileZoom), WebMercator.longitude(worldX, tileZoom))
    }

    /** The map moved by ([dx], [dy]) pixels (a finger drag): its content follows the finger. */
    fun panned(dx: Double, dy: Double): MapViewport {
        val (lat, lon) = fromScreen(width / 2 - dx, height / 2 - dy)
        return MapViewport(lat.coerceIn(-MAX_LATITUDE, MAX_LATITUDE), wrap(lon), zoom, width, height)
    }

    /** At [newZoom], with the coordinate under ([x], [y]) staying where it is (a pinch around the fingers). */
    fun zoomedAround(newZoom: Double, x: Double, y: Double): MapViewport {
        val clamped = newZoom.coerceIn(MIN_ZOOM.toDouble(), MAX_ZOOM.toDouble())
        val (lat, lon) = fromScreen(x, y)
        val zoomed = MapViewport(centerLatitude, centerLongitude, clamped, width, height)
        // Where the pinned coordinate is now, compared to where it should stay: move the map by the difference.
        val (nowX, nowY) = zoomed.project(lat, lon)
        return zoomed.panned(x - nowX, y - nowY)
    }

    /** This view, with the new [width] x [height] (rotation, resize). */
    fun resized(width: Double, height: Double) = MapViewport(centerLatitude, centerLongitude, zoom, width, height)

    /** The tiles that cover the view, with the position of their top left corner on it. Columns wrap around the date line. */
    val tiles: List<MapTile> by lazy {
        val count = 1 shl tileZoom
        val firstX = floor((centerX - width / 2 / scale) / WebMercator.TILE_SIZE).toInt()
        val lastX = floor((centerX + width / 2 / scale) / WebMercator.TILE_SIZE).toInt()
        val firstY = floor((centerY - height / 2 / scale) / WebMercator.TILE_SIZE).toInt()
        val lastY = floor((centerY + height / 2 / scale) / WebMercator.TILE_SIZE).toInt()
        buildList {
            for (ty in firstY..lastY) {
                if (ty < 0 || ty >= count) continue
                for (tx in firstX..lastX) {
                    add(
                        MapTile(
                            x = ((tx % count) + count) % count,
                            y = ty,
                            zoom = tileZoom,
                            left = (tx * WebMercator.TILE_SIZE - centerX) * scale + width / 2,
                            top = (ty * WebMercator.TILE_SIZE - centerY) * scale + height / 2
                        )
                    )
                }
            }
        }
    }

    companion object {
        const val MIN_ZOOM = 3
        const val MAX_ZOOM = 19
        private const val MAX_LATITUDE = 85.0

        /** The zoom change of a pinch: a spread by factor 2 is one zoom level. */
        fun zoomAfterPinch(zoom: Double, pinchFactor: Double): Double = zoom + ln(pinchFactor) / ln(2.0)

        private fun wrap(longitude: Double): Double {
            var lon = (longitude + 180.0) % 360.0
            if (lon < 0) lon += 360.0
            return lon - 180.0
        }
    }
}
