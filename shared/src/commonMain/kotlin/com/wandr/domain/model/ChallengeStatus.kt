package com.wandr.domain.model

/**
 * Lifecycle state of a challenge as shown to the user.
 *
 * The database stores only two values (`challenge_status`): [DRAFT] (not published yet, only the creator sees
 * it) and [ACTIVE] (published). Everything else is derived at runtime from that flag and the dates, see
 * `EvaluateChallengeStatusUseCase`:
 * - [PLANNED]: published, but the start date is in the future
 * - [ACTIVE]: published and running
 * - [COMPLETED] / [EXPIRED]: over, with / without reaching the goal
 */
enum class ChallengeStatus(val value: String) {
    DRAFT("draft"),
    PLANNED("planned"),
    ACTIVE("active"),
    COMPLETED("completed"),
    EXPIRED("expired");

    companion object {
        /** The value that is persisted: only draft or active exist in the database. */
        fun storedValue(isActive: Boolean): String = if (isActive) ACTIVE.value else DRAFT.value
    }
}
