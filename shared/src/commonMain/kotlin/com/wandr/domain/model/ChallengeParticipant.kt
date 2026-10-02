package com.wandr.domain.model

data class ChallengeParticipant(
    val id: String,
    val challengeId: String,
    val userId: String,
    /** The team this participant contributes for in a group challenge; null in an individual challenge. */
    val teamId: String?,
    val progressValue: Double,
    val isCompleted: Boolean,
    val joinedAt: Long,
    val completedAt: Long?
)
