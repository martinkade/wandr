package com.wandr.domain.geo

import com.wandr.domain.model.GpsTrackpoint

/** Live figures of a recording that are derived from the track. */
object RecordingMetrics {
    /** Below this a pace is meaningless: the user is standing still (GPS noise only). */
    private const val MIN_WINDOW_DISTANCE_METERS = 5.0

    /**
     * The current pace in seconds per kilometer, averaged over the last [windowMillis] of [points] (by their
     * timestamps), or null while there is not enough movement.
     */
    fun recentPaceSecondsPerKm(points: List<GpsTrackpoint>, windowMillis: Long = 30_000L): Double? {
        if (points.size < 2) return null
        val last = points.last()
        var first = points.lastIndex
        while (first > 0 && last.timestamp - points[first - 1].timestamp <= windowMillis) first--
        if (first == points.lastIndex) return null

        var distance = 0.0
        for (i in first until points.lastIndex) {
            distance += GeoMath.distanceMeters(points[i].latitude, points[i].longitude, points[i + 1].latitude, points[i + 1].longitude)
        }
        val seconds = (last.timestamp - points[first].timestamp) / 1000.0
        if (distance < MIN_WINDOW_DISTANCE_METERS || seconds <= 0) return null
        return seconds / (distance / 1000.0)
    }
}

/**
 * Elevation gain from noisy GPS altitudes: a change only counts once it exceeds [thresholdMeters] against the last
 * accepted altitude, so jitter does not add up to phantom climbs.
 */
class ElevationGainTracker(private val thresholdMeters: Double = 3.0) {
    private var anchor: Double? = null

    /** Feeds the next altitude and returns the gain it adds (0 for flat, down or not yet significant changes). */
    fun add(altitudeMeters: Double): Double {
        val a = anchor
        if (a == null) {
            anchor = altitudeMeters
            return 0.0
        }
        return when {
            altitudeMeters - a > thresholdMeters -> (altitudeMeters - a).also { anchor = altitudeMeters }
            a - altitudeMeters > thresholdMeters -> 0.0.also { anchor = altitudeMeters }
            else -> 0.0
        }
    }

    fun reset() {
        anchor = null
    }
}
