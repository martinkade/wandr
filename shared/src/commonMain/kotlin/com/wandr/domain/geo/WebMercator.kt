package com.wandr.domain.geo

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/** The Web Mercator projection of slippy maps (OpenStreetMap): coordinates <-> pixels of the whole world map at a zoom. */
internal object WebMercator {
    const val TILE_SIZE = 256
    private const val MAX_LATITUDE = 85.0511

    fun worldSize(zoom: Int): Double = TILE_SIZE * 2.0.pow(zoom)

    fun worldX(longitude: Double, zoom: Int): Double = worldSize(zoom) * ((longitude + 180.0) / 360.0)

    fun worldY(latitude: Double, zoom: Int): Double {
        val lat = latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE) * PI / 180.0
        return worldSize(zoom) * (0.5 - ln(tan(PI / 4 + lat / 2)) / (2 * PI))
    }

    fun longitude(worldX: Double, zoom: Int): Double = worldX / worldSize(zoom) * 360.0 - 180.0

    fun latitude(worldY: Double, zoom: Int): Double {
        val n = PI - 2 * PI * worldY / worldSize(zoom)
        return (atan((exp(n) - exp(-n)) / 2)) * 180.0 / PI
    }
}
