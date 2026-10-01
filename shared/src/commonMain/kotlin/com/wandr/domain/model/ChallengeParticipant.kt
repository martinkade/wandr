package com.wandr.domain.model

data class ChallengeParticipant(
    val id: String,
    val challengeId: String,
    val userId: String,
    val progressValue: Double,
    val isCompleted: Boolean,
    val joinedAt: Long,
    val completedAt: Long?
)
