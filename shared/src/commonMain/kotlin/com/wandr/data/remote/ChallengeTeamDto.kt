package com.wandr.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Insert payload for `public.challenge_teams`; a database trigger then adds the team's members as participants. */
@Serializable
data class ChallengeTeamInsertDto(
    val id: String,
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("team_id") val teamId: String,
    @SerialName("enrolled_by") val enrolledBy: String
)

/** Insert payload for `public.challenge_participants` (individual challenges only; group rows come from triggers). */
@Serializable
data class ChallengeParticipantInsertDto(
    val id: String,
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("user_id") val userId: String
)

/** Row returned by the `challenge_team_standings` function. */
@Serializable
data class TeamStandingDto(
    @SerialName("team_id") val teamId: String,
    @SerialName("team_name") val teamName: String,
    @SerialName("team_avatar_url") val teamAvatarUrl: String? = null,
    @SerialName("total_progress") val totalProgress: Double = 0.0,
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("completed_count") val completedCount: Int = 0
)

/** Row of `public.challenge_participants`. */
@Serializable
data class ChallengeParticipantDto(
    val id: String,
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("team_id") val teamId: String? = null,
    @SerialName("progress_value") val progressValue: Double = 0.0,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("joined_at") val joinedAt: String,
    @SerialName("completed_at") val completedAt: String? = null
) {
    fun toEntity(syncStatus: String = "SYNCED") = com.wandr.data.local.entity.ChallengeParticipantEntity(
        id = id,
        challengeId = challengeId,
        userId = userId,
        teamId = teamId,
        progressValue = progressValue,
        isCompleted = isCompleted,
        joinedAt = kotlin.time.Instant.parse(joinedAt).toEpochMilliseconds(),
        completedAt = completedAt?.let { kotlin.time.Instant.parse(it).toEpochMilliseconds() },
        syncStatus = syncStatus
    )
}
