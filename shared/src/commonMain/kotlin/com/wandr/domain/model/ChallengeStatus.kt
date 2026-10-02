package com.wandr.domain.model

/**
 * Lifecycle state of a challenge.
 *
 * Only [DRAFT] and [ACTIVE] exist in the database (`challenge_status`). [COMPLETED] and [EXPIRED] are never
 * stored: they are derived at runtime from the start / end date (and, for [COMPLETED], the participants'
 * progress), see `EvaluateChallengeStatusUseCase`.
 */
enum class ChallengeStatus(val value: String) {
    DRAFT("draft"),
    ACTIVE("active"),
    COMPLETED("completed"),
    EXPIRED("expired");

    companion object {
        /** The value to persist when saving a challenge that starts at [startDate]; only draft / active exist. */
        fun storedValueAt(startDate: Long, nowMillis: Long): String =
            if (nowMillis < startDate) DRAFT.value else ACTIVE.value
    }
}
