package com.wandr.android.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Formats epoch-millisecond timestamps (as stored in the shared domain models) for display, using the user's
 * locale and time zone. Pass the current locale from `LocalConfiguration` in composables so the text updates
 * when the system language changes.
 */
object AppDateFormatter {

    /** Date only, e.g. "Oct 1, 2026" (en) or "01.10.2026" (de), depending on [style]. */
    fun formatDate(
        epochMillis: Long,
        style: FormatStyle = FormatStyle.MEDIUM,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault()
    ): String = DateTimeFormatter.ofLocalizedDate(style)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    /** Date and time, e.g. for activity start times. */
    fun formatDateTime(
        epochMillis: Long,
        dateStyle: FormatStyle = FormatStyle.MEDIUM,
        timeStyle: FormatStyle = FormatStyle.SHORT,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault()
    ): String = DateTimeFormatter.ofLocalizedDateTime(dateStyle, timeStyle)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(epochMillis).atZone(zone))
}
