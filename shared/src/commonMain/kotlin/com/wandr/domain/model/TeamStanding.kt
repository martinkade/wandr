package com.wandr.domain.model

/**
 * One team in a group challenge. Only aggregates are exposed: the progress of single members of *other* teams
 * stays private (see `challenge_team_standings` in SUPABASE.md).
 */
data class TeamStanding(
    val rank: Int,
    val teamId: String,
    val teamName: String,
    val avatarUrl: String?,
    /** Sum of the progress of all participating members, in meters / seconds. */
    val totalProgress: Double,
    /** [totalProgress] relative to the challenge target, 0..100. */
    val progressPercentage: Double,
    val memberCount: Int,
    val completedMemberCount: Int,
    val isCompleted: Boolean
)
