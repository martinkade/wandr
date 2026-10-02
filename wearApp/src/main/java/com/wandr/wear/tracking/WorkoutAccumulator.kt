package com.wandr.wear.tracking

import com.wandr.domain.watch.WatchTrackpoint
import com.wandr.domain.watch.WatchWorkout
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Turns Health Services samples into the numbers of a [WatchWorkout]. Pure Kotlin (no Android types).
 * Distance and elevation prefer the totals reported by the platform; if it reports none, they are
 * derived from the recorded trackpoints.
 */
class WorkoutAccumulator(private val minPointIntervalMs: Long = 1_000L) {
    private var reportedDistance = 0.0
    private var reportedElevation = 0.0
    private var hrSum = 0L
    private var hrCount = 0
    private var hrMax = 0
    private val points = mutableListOf<WatchTrackpoint>()

    var currentHeartRate: Int? = null
        private set

    val trackpoints: List<WatchTrackpoint> get() = points

    val distanceMeters: Double
        get() = if (reportedDistance > 0.0) reportedDistance else derivedDistance()

    val elevationGainMeters: Double
        get() = if (reportedElevation > 0.0) reportedElevation else derivedElevationGain()

    /** Cumulative distance since the start; totals never decrease. */
    fun onDistanceTotal(meters: Double) {
        if (meters.isFinite() && meters > reportedDistance) reportedDistance = meters
    }

    fun onElevationGainTotal(meters: Double) {
        if (meters.isFinite() && meters > reportedElevation) reportedElevation = meters
    }

    fun onHeartRate(bpm: Double) {
        if (!bpm.isFinite() || bpm < 20 || bpm > 250) return
        val value = bpm.toInt()
        currentHeartRate = value
        hrSum += value
        hrCount++
        hrMax = max(hrMax, value)
    }

    /** Returns true if the point was recorded (valid and not too close in time to the previous one). */
    fun onLocation(latitude: Double, longitude: Double, altitude: Double, timestampMs: Long): Boolean {
        if (!latitude.isFinite() || !longitude.isFinite()) return false
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
        val last = points.lastOrNull()
        if (last != null && timestampMs - last.timestamp < minPointIntervalMs) return false
        val alt = if (altitude.isFinite()) altitude else last?.altitudeMeters ?: 0.0
        val speed = if (last != null && timestampMs > last.timestamp) {
            val dt = (timestampMs - last.timestamp) / 1000.0
            (haversine(last.latitude, last.longitude, latitude, longitude) / dt).toFloat()
        } else 0f
        points += WatchTrackpoint(latitude, longitude, alt, timestampMs, speed)
        return true
    }

    fun build(id: String, type: String, startTimeMs: Long, endTimeMs: Long): WatchWorkout = WatchWorkout(
        id = id,
        activityType = type,
        startTime = startTimeMs,
        endTime = max(endTimeMs, startTimeMs),
        distanceMeters = distanceMeters,
        elevationGainMeters = elevationGainMeters,
        averageHeartRate = if (hrCount > 0) (hrSum / hrCount).toInt() else null,
        maxHeartRate = if (hrCount > 0) hrMax else null,
        trackpoints = points.toList()
    )

    private fun derivedDistance(): Double =
        points.zipWithNext { a, b -> haversine(a.latitude, a.longitude, b.latitude, b.longitude) }.sum()

    /** Sum of ascents; altitude jitter below [ELEVATION_THRESHOLD_M] is ignored. */
    private fun derivedElevationGain(): Double {
        var gain = 0.0
        var reference = points.firstOrNull()?.altitudeMeters ?: return 0.0
        for (p in points) {
            val diff = p.altitudeMeters - reference
            if (diff >= ELEVATION_THRESHOLD_M) {
                gain += diff
                reference = p.altitudeMeters
            } else if (diff <= -ELEVATION_THRESHOLD_M) {
                reference = p.altitudeMeters
            }
        }
        return gain
    }

    companion object {
        const val ELEVATION_THRESHOLD_M = 2.0
        private const val EARTH_RADIUS_M = 6_371_000.0

        fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
            return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(a)))
        }
    }
}
