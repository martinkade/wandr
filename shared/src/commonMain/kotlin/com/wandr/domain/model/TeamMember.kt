package com.wandr.domain.model

data class TeamMember(
    val id: String,
    val teamId: String,
    val userId: String,
    val role: TeamRole,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val joinedAt: Long
)
