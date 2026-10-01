package com.wandr.domain.model

data class TeamMember(
    val id: String,
    val teamId: String,
    val userId: String,
    val role: String, // admin, member
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val joinedAt: Long
)
