package com.wandr.android.ui.challenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.wandr.android.R
import com.wandr.android.util.AppDateFormatter
import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeStatus
import com.wandr.domain.model.ChallengeType
import com.wandr.domain.model.DurationUnit
import com.wandr.domain.model.duration
import com.wandr.presentation.challenge.ChallengeUnits
import java.text.NumberFormat

internal fun challengeUnitRes(type: ChallengeType) = when (type) {
    ChallengeType.DISTANCE -> R.string.challenge_unit_distance
    ChallengeType.ELEVATION -> R.string.challenge_unit_elevation
    ChallengeType.TIME -> R.string.challenge_unit_time
}

/** A stored challenge value (meters / seconds) as text in friendly units, e.g. "100 km" or "5,000 m". */
@Composable
internal fun challengeValueText(type: ChallengeType, baseValue: Double): String {
    val locale = LocalConfiguration.current.locales[0]
    val number = remember(type, baseValue, locale) {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }
            .format(ChallengeUnits.toDisplay(type, baseValue))
    }
    return "$number ${stringResource(challengeUnitRes(type))}"
}

/** "1 Oct 2026, 10:00 – 31 Oct 2026, 10:00 (30 days)": start, end and the length in hours (< 1 day) or days. */
@Composable
internal fun challengePeriodText(challenge: Challenge): String {
    val locale = LocalConfiguration.current.locales[0]
    val start = remember(challenge.startDate, locale) { AppDateFormatter.formatDateTime(challenge.startDate, locale = locale) }
    val end = remember(challenge.endDate, locale) { AppDateFormatter.formatDateTime(challenge.endDate, locale = locale) }
    val duration = challenge.duration
    val length = when (duration.unit) {
        DurationUnit.DAYS -> pluralStringResource(R.plurals.challenge_duration_days, duration.value.toInt(), duration.value.toInt())
        DurationUnit.HOURS -> pluralStringResource(R.plurals.challenge_duration_hours, duration.value.toInt(), duration.value.toInt())
    }
    return "$start – $end ($length)"
}

/** How long until something happens, in the largest sensible unit: days from a day on, otherwise hours (at least 1). */
internal data class RemainingTime(val value: Int, val isDays: Boolean) {
    companion object {
        private const val HOUR = 3_600_000L
        private const val DAY = 24 * HOUR

        fun of(millis: Long): RemainingTime {
            val positive = millis.coerceAtLeast(0)
            if (positive >= DAY) return RemainingTime(((positive + DAY - 1) / DAY).toInt(), true)
            val hours = ((positive + HOUR - 1) / HOUR).toInt().coerceAtLeast(1)
            // Just under a day rounds up to "1 day", not to "24 hours".
            return if (hours >= 24) RemainingTime(1, true) else RemainingTime(hours, false)
        }
    }
}

/** "Ends in 3 days", "Starts in 2 hours" or "Ended" for the runtime [status]; null for a draft (it has no schedule yet). */
@Composable
internal fun challengeRemainingText(challenge: Challenge, status: ChallengeStatus): String? {
    val now = remember(challenge.id, status) { System.currentTimeMillis() }
    return when (status) {
        ChallengeStatus.DRAFT -> null
        ChallengeStatus.COMPLETED, ChallengeStatus.EXPIRED -> stringResource(R.string.challenge_ended)
        ChallengeStatus.PLANNED -> RemainingTime.of(challenge.startDate - now).let {
            if (it.isDays) pluralStringResource(
                R.plurals.challenge_starts_in_days,
                it.value,
                it.value
            )
            else pluralStringResource(R.plurals.challenge_starts_in_hours, it.value, it.value)
        }

        ChallengeStatus.ACTIVE -> RemainingTime.of(challenge.endDate - now).let {
            if (it.isDays) pluralStringResource(
                R.plurals.challenge_ends_in_days,
                it.value,
                it.value
            )
            else pluralStringResource(R.plurals.challenge_ends_in_hours, it.value, it.value)
        }
    }
}

/** "30 days" / "5 hours": the length of the challenge. */
@Composable
internal fun challengeLengthText(challenge: Challenge): String {
    val duration = challenge.duration
    return when (duration.unit) {
        DurationUnit.DAYS -> pluralStringResource(
            R.plurals.challenge_duration_days,
            duration.value.toInt(),
            duration.value.toInt()
        )

        DurationUnit.HOURS -> pluralStringResource(
            R.plurals.challenge_duration_hours,
            duration.value.toInt(),
            duration.value.toInt()
        )
    }
}
