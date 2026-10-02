package com.wandr.domain.model

enum class DurationUnit { HOURS, DAYS }

/** A challenge's length for display: [value] in whole [unit]s. */
data class ChallengeDuration(val value: Long, val unit: DurationUnit)

private const val HOUR_MILLIS = 60 * 60 * 1000L
private const val DAY_MILLIS = 24 * HOUR_MILLIS

/**
 * Length of a challenge for display: hours when it is shorter than a day (at least 1), otherwise days, rounded to
 * the nearest whole day (at least 1), so "Mon 00:00 to Sun 23:59" reads as 7 days rather than 6.
 */
fun challengeDuration(startDate: Long, endDate: Long): ChallengeDuration {
    val length = (endDate - startDate).coerceAtLeast(0L)
    return if (length < DAY_MILLIS) {
        ChallengeDuration(((length + HOUR_MILLIS / 2) / HOUR_MILLIS).coerceAtLeast(1L), DurationUnit.HOURS)
    } else {
        ChallengeDuration(((length + DAY_MILLIS / 2) / DAY_MILLIS).coerceAtLeast(1L), DurationUnit.DAYS)
    }
}

val Challenge.duration: ChallengeDuration get() = challengeDuration(startDate, endDate)
