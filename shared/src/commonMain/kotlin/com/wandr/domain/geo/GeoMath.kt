package com.wandr.domain.geo

import com.wandr.domain.model.GpsTrackpoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Great-circle distance (haversine) between two coordinates in meters. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = (lat2 - lat1) * (PI / 180.0)
        val dLon = (lon2 - lon1) * (PI / 180.0)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * (PI / 180.0)) * cos(lat2 * (PI / 180.0)) * sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Initial bearing (heading) in degrees (0..360) from (lat1, lon1) to (lat2, lon2). */
    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = lat1 * (PI / 180.0)
        val phi2 = lat2 * (PI / 180.0)
        val dLon = (lon2 - lon1) * (PI / 180.0)
        val y = sin(dLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
        val bearing = (atan2(y, x) * (180.0 / PI)).toFloat()
        return (bearing + 360f) % 360f
    }
}

/** Calculates moving heading from a track (bearing between last point and an earlier point >= 2m away). */
fun List<GpsTrackpoint>.movingHeading(): Float {
    if (size < 2) return 0f
    val last = last()
    val prev = reversed().drop(1).firstOrNull {
        GeoMath.distanceMeters(it.latitude, it.longitude, last.latitude, last.longitude) >= 2.0
    } ?: return 0f
    return GeoMath.bearingDegrees(prev.latitude, prev.longitude, last.latitude, last.longitude)
}
