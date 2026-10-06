package com.wandr.android.ui.activity

import java.util.Locale
import kotlin.math.roundToLong

/** Number formatting for the activity cards and details. Locale-neutral where possible (no translated units). */
internal object ActivityFormat {
    /** "51:07" below an hour, "1:05:07" from an hour on. */
    fun duration(seconds: Double): String {
        val total = seconds.toLong().coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%d:%02d", m, s)
    }

    /** "01:05:07": the clock of a running recording. */
    fun clock(seconds: Double): String {
        val total = seconds.toLong().coerceAtLeast(0)
        return String.format(Locale.ROOT, "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
    }

    /** A pace of [secondsPerKm] as "5:35", or "-:--" while there is none. */
    fun paceOrDash(secondsPerKm: Double?): String {
        if (secondsPerKm == null || secondsPerKm <= 0 || secondsPerKm.isNaN()) return "-:--"
        val total = secondsPerKm.roundToLong()
        return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60)
    }

    /** The speed in km/h that belongs to a pace in seconds per kilometer, or "-" while there is none. */
    fun speedFromPace(secondsPerKm: Double?): String {
        if (secondsPerKm == null || secondsPerKm <= 0 || secondsPerKm.isNaN()) return "-"
        return String.format(Locale.ROOT, "%.1f", 3600.0 / secondsPerKm)
    }

    /** Minutes per kilometer as "5:35", or null without a usable distance. */
    fun pace(distanceMeters: Double, durationSeconds: Double): String? {
        if (distanceMeters < MIN_DISTANCE_METERS || durationSeconds <= 0) return null
        val secondsPerKm = (durationSeconds / (distanceMeters / 1000.0)).roundToLong()
        return String.format(Locale.ROOT, "%d:%02d", secondsPerKm / 60, secondsPerKm % 60)
    }

    /** Kilometers per hour with one decimal, or null without a usable distance. */
    fun speedKmh(distanceMeters: Double, durationSeconds: Double): String? {
        if (distanceMeters < MIN_DISTANCE_METERS || durationSeconds <= 0) return null
        return String.format(Locale.ROOT, "%.1f", distanceMeters / 1000.0 / (durationSeconds / 3600.0))
    }

    /** Kilometers with two decimals below 100 km, one above. */
    fun distanceKm(distanceMeters: Double): String =
        String.format(Locale.ROOT, if (distanceMeters >= 100_000) "%.1f" else "%.2f", distanceMeters / 1000.0)

    /** Below this a pace is meaningless (GPS noise on a standing recording). */
    private const val MIN_DISTANCE_METERS = 50.0
}
