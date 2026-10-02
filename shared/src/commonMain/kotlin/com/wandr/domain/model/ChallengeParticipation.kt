package com.wandr.domain.model

/**
 * The signed-in user's participation in one challenge. [teamId] is the team the user contributes for in a group
 * challenge (the team is enrolled) and null in an individual challenge.
 */
data class ChallengeParticipation(val challengeId: String, val teamId: String?)
