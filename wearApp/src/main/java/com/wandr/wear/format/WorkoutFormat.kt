package com.wandr.wear.format

import java.util.Locale

object WorkoutFormat {
    /** `m:ss` below one hour, `h:mm:ss` otherwise. */
    fun duration(ms: Long): String {
        val total = (ms.coerceAtLeast(0L)) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.ROOT, "%d:%02d", m, s)
    }

    /** Kilometers with two decimals, using the decimal separator of [locale]. */
    fun kilometers(meters: Double, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%.2f", meters.coerceAtLeast(0.0) / 1000.0)
}
