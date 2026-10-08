package com.wandr.domain.geo

import com.wandr.domain.model.GpsTrackpoint
import kotlin.math.max

/**
 * Cleans the GPS fixes of a recording before they become track points, so the route is not wiggly (hiking, slow walking:
 * the error of a fix is as large as a step):
 *
 * 1. fixes with a poor accuracy ([maxAccuracyMeters]) are dropped;
 * 2. the position is the average of the last [windowSize] fixes, each weighted by 1 / accuracy², so precise fixes count
 *    more and single outliers are pulled back towards the others;
 * 3. a point is only added once it lies [stepFor] away from the previous one: standing still or tiny jitter around a
 *    spot adds no points (and no distance).
 *
 * The average lags about one fix behind; at hiking pace that is a few meters.
 */
class TrackFilter(
    private val maxAccuracyMeters: Float = DEFAULT_MAX_ACCURACY_METERS,
    private val windowSize: Int = DEFAULT_WINDOW,
    private val minStepMeters: Double = DEFAULT_MIN_STEP_METERS,
    /** Additional distance a new point must have per meter of inaccuracy (an uncertain signal needs a bigger step). */
    private val stepPerAccuracy: Double = DEFAULT_STEP_PER_ACCURACY
) {
    private class Fix(val point: GpsTrackpoint, val weight: Double)

    private val window = ArrayDeque<Fix>()
    private var lastEmitted: GpsTrackpoint? = null

    /** Forgets the history, e.g. at the start or after a pause (the way in between is not part of the track). */
    fun reset() {
        window.clear()
        lastEmitted = null
    }

    /**
     * Feeds one fix. Returns the smoothed point to add to the track, or null when the fix is too inaccurate or the
     * position has not really changed.
     *
     * @param accuracyMeters horizontal accuracy of the fix (1 sigma); null when unknown, then a middling value is assumed
     */
    fun accept(point: GpsTrackpoint, accuracyMeters: Float?): GpsTrackpoint? {
        val accuracy = (accuracyMeters ?: UNKNOWN_ACCURACY_METERS).coerceAtLeast(MIN_ACCURACY_METERS)
        if (accuracy > maxAccuracyMeters) return null

        window.addLast(Fix(point, 1.0 / (accuracy.toDouble() * accuracy)))
        while (window.size > windowSize) window.removeFirst()

        var weights = 0.0
        var lat = 0.0
        var lon = 0.0
        var altitude = 0.0
        for (fix in window) {
            weights += fix.weight
            lat += fix.point.latitude * fix.weight
            lon += fix.point.longitude * fix.weight
            altitude += fix.point.altitudeMeters * fix.weight
        }
        val smoothed = point.copy(latitude = lat / weights, longitude = lon / weights, altitudeMeters = altitude / weights)

        val last = lastEmitted
        if (last != null && GeoMath.distanceMeters(last.latitude, last.longitude, smoothed.latitude, smoothed.longitude) < stepFor(accuracy)) {
            return null
        }
        lastEmitted = smoothed
        return smoothed
    }

    /** How far a new point has to be from the previous one: more for an uncertain signal. */
    private fun stepFor(accuracyMeters: Float): Double = max(minStepMeters, accuracyMeters * stepPerAccuracy)

    companion object {
        const val DEFAULT_MAX_ACCURACY_METERS = 30f
        const val DEFAULT_WINDOW = 4
        const val DEFAULT_MIN_STEP_METERS = 3.0
        const val DEFAULT_STEP_PER_ACCURACY = 0.4
        private const val UNKNOWN_ACCURACY_METERS = 10f
        private const val MIN_ACCURACY_METERS = 1f
    }
}
