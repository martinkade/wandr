package com.wandr.domain.model

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val progressValue: Double,
    val progressPercentage: Double,
    val isCompleted: Boolean
)
