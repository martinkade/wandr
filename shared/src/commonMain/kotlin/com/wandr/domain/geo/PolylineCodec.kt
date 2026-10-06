package com.wandr.domain.geo

import com.wandr.domain.model.GpsTrackpoint
import kotlin.math.roundToLong

/**
 * Google's Encoded Polyline Algorithm (precision 5, i.e. about 1 m):
 * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 * A route of a few hundred points becomes a short ASCII string that can be stored with the activity.
 */
object PolylineCodec {
    private const val FACTOR = 1e5

    fun encode(points: List<GpsTrackpoint>): String {
        val out = StringBuilder()
        var lastLat = 0L
        var lastLon = 0L
        for (point in points) {
            val lat = (point.latitude * FACTOR).roundToLong()
            val lon = (point.longitude * FACTOR).roundToLong()
            appendValue(out, lat - lastLat)
            appendValue(out, lon - lastLon)
            lastLat = lat
            lastLon = lon
        }
        return out.toString()
    }

    /** The decoded points only have coordinates (time, altitude and speed are not part of a polyline). */
    fun decode(encoded: String): List<GpsTrackpoint> {
        val points = mutableListOf<GpsTrackpoint>()
        var index = 0
        var lat = 0L
        var lon = 0L
        while (index < encoded.length) {
            val (dLat, afterLat) = readValue(encoded, index) ?: break
            val (dLon, afterLon) = readValue(encoded, afterLat) ?: break
            index = afterLon
            lat += dLat
            lon += dLon
            points += GpsTrackpoint(lat / FACTOR, lon / FACTOR, altitudeMeters = 0.0, timestamp = 0L)
        }
        return points
    }

    private fun appendValue(out: StringBuilder, value: Long) {
        var v = if (value < 0) (value shl 1).inv() else value shl 1
        while (v >= 0x20) {
            out.append((((v and 0x1f) or 0x20) + 63).toInt().toChar())
            v = v shr 5
        }
        out.append((v + 63).toInt().toChar())
    }

    /** The decoded value and the index after it, or null if the input ends in the middle of a value. */
    private fun readValue(encoded: String, start: Int): Pair<Long, Int>? {
        var result = 0L
        var shift = 0
        var index = start
        while (true) {
            if (index >= encoded.length) return null
            val b = encoded[index++].code - 63
            result = result or ((b and 0x1f).toLong() shl shift)
            shift += 5
            if (b < 0x20) break
        }
        return (if (result and 1L != 0L) (result shr 1).inv() else result shr 1) to index
    }
}
