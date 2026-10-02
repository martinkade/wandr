package com.wandr.domain.model

/**
 * `challenge_scope`:
 * - INDIVIDUAL: open to every user; everybody takes part for themselves.
 * - GROUP: teams compete against other teams. Team owners / admins enroll their team (`challenge_teams`); all
 *   members then contribute to their team's result, and the standings rank teams, not members. The members of
 *   one team do not compete against each other.
 */
enum class ChallengeScope(val value: String) {
    INDIVIDUAL("individual"),
    GROUP("group");

    companion object {
        fun fromValue(value: String?): ChallengeScope = entries.firstOrNull { it.value == value } ?: INDIVIDUAL
    }
}
