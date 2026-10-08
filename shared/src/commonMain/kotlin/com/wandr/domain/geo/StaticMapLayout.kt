package com.wandr.domain.geo

import com.wandr.domain.model.GpsTrackpoint
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/** One map tile (slippy map scheme, as used by OpenStreetMap) and where its top left corner lies in the viewport. */
data class MapTile(val x: Int, val y: Int, val zoom: Int, val left: Double, val top: Double) {
    /** Where the platforms load the tile from; the one place to change when another tile provider is used. */
    val url: String
        get() = StaticMapLayout.TILE_URL_TEMPLATE.replace("{z}", "$zoom").replace("{x}", "$x")
            .replace("{y}", "$y")
}

/**
 * Which tiles of a slippy map (Web Mercator, 256 px tiles) fill a viewport that shows a route, and where the route's
 * points lie in that viewport. Platform independent: the platforms only load the tiles and draw.
 */
class StaticMapLayout private constructor(
    val zoom: Int,
    private val originX: Double,
    private val originY: Double,
    val width: Double,
    val height: Double
) {
    /** Position of a coordinate in the viewport, in pixels. */
    fun project(latitude: Double, longitude: Double): Pair<Double, Double> =
        worldX(longitude, zoom) - originX to worldY(latitude, zoom) - originY

    /** The tiles that cover the viewport. Tile columns wrap around the date line; rows outside the world are left out. */
    val tiles: List<MapTile> by lazy {
        val count = 1 shl zoom
        val firstX = floor(originX / TILE_SIZE).toInt()
        val lastX = floor((originX + width - 1e-6) / TILE_SIZE).toInt()
        val firstY = floor(originY / TILE_SIZE).toInt()
        val lastY = floor((originY + height - 1e-6) / TILE_SIZE).toInt()
        buildList {
            for (ty in firstY..lastY) {
                if (ty < 0 || ty >= count) continue
                for (tx in firstX..lastX) {
                    add(
                        MapTile(
                            ((tx % count) + count) % count,
                            ty,
                            zoom,
                            tx * TILE_SIZE - originX,
                            ty * TILE_SIZE - originY
                        )
                    )
                }
            }
        }
    }

    companion object {
        const val TILE_SIZE = 256

        /** The public OpenStreetMap tile server: for light use only, replace it by a tile provider for a wide release. */
        const val TILE_URL_TEMPLATE = "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
        const val MAX_ZOOM = 17
        private const val MIN_ZOOM = 2

        /**
         * The most detailed zoom at which the whole route fits into a viewport of [width] x [height] pixels with
         * [padding] around it, and the route centered in the free space (the top and bottom padding can differ, e.g. when a
         * bar covers the top of the viewport). Fewer than two points (or a route of one spot) show a
         * [MAX_ZOOM] view of the first point.
         */
        fun fit(
            points: List<GpsTrackpoint>,
            width: Double,
            height: Double,
            padding: Double = 0.0,
            topPadding: Double = padding,
            bottomPadding: Double = padding
        ): StaticMapLayout {
            if (points.isEmpty()) return StaticMapLayout(MIN_ZOOM, 0.0, 0.0, width, height)
            val minLat = points.minOf { it.latitude }
            val maxLat = points.maxOf { it.latitude }
            val minLon = points.minOf { it.longitude }
            val maxLon = points.maxOf { it.longitude }
            val availableW = (width - 2 * padding).coerceAtLeast(1.0)
            val availableH = (height - topPadding - bottomPadding).coerceAtLeast(1.0)

            val zoom = (MAX_ZOOM downTo MIN_ZOOM).firstOrNull { z ->
                worldX(maxLon, z) - worldX(minLon, z) <= availableW && worldY(minLat, z) - worldY(
                    maxLat,
                    z
                ) <= availableH
            } ?: MIN_ZOOM
            val centerX = (worldX(minLon, zoom) + worldX(maxLon, zoom)) / 2
            val centerY = (worldY(minLat, zoom) + worldY(maxLat, zoom)) / 2
            // The route's center goes to the center of the free space.
            return StaticMapLayout(
                zoom,
                centerX - width / 2,
                centerY - (topPadding + availableH / 2),
                width,
                height
            )
        }

        private fun worldX(longitude: Double, zoom: Int): Double = WebMercator.worldX(longitude, zoom)

        private fun worldY(latitude: Double, zoom: Int): Double = WebMercator.worldY(latitude, zoom)
    }
}
