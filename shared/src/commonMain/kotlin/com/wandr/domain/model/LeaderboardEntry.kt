package com.wandr.domain.model

/** One member in the ranking of a challenge. Equal progress shares a rank (1, 2, 2, 4). */
data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val progressValue: Double,
    /** [progressValue] relative to the challenge target, 0..100. */
    val progressPercentage: Double,
    val isCompleted: Boolean
)
