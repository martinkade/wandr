package com.wandr.data.remote

import com.wandr.data.local.entity.TeamEntity
import com.wandr.data.local.entity.TeamMemberEntity
import com.wandr.domain.model.TeamRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Instant

/** Row of `public.teams`; timestamps are ISO-8601 `TIMESTAMPTZ` strings. */
@Serializable
data class TeamDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("invite_code") val inviteCode: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
) {
    fun toEntity(syncStatus: String = "SYNCED") = TeamEntity(
        id = id,
        name = name,
        description = description,
        avatarUrl = avatarUrl,
        coverUrl = coverUrl,
        inviteCode = inviteCode,
        createdBy = createdBy,
        createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
        updatedAt = Instant.parse(updatedAt).toEpochMilliseconds(),
        syncStatus = syncStatus
    )
}

fun TeamEntity.toDto() = TeamDto(
    id = id,
    name = name,
    description = description,
    avatarUrl = avatarUrl,
    coverUrl = coverUrl,
    inviteCode = inviteCode,
    createdBy = createdBy,
    createdAt = Instant.fromEpochMilliseconds(createdAt).toString(),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt).toString()
)

/** Row of `public.team_members`. */
@Serializable
data class TeamMemberDto(
    val id: String,
    @SerialName("team_id") val teamId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("joined_at") val joinedAt: String,
    /** Assigned by the server when the membership is created (appended at the end). */
    val priority: Int = 0
) {
    fun toEntity(syncStatus: String = "SYNCED") = TeamMemberEntity(
        id = id,
        teamId = teamId,
        userId = userId,
        role = TeamRole.fromValue(role),
        joinedAt = Instant.parse(joinedAt).toEpochMilliseconds(),
        priority = priority,
        syncStatus = syncStatus
    )
}

fun TeamMemberEntity.toDto() = TeamMemberDto(
    id = id,
    teamId = teamId,
    userId = userId,
    role = role.value,
    joinedAt = Instant.fromEpochMilliseconds(joinedAt).toString(),
    priority = priority
)

/**
 * Payload for `UPDATE public.teams`. Built as a [JsonObject] so cleared values (description, images) are sent as
 * explicit JSON `null`. Never an upsert: that would need the INSERT policy, which only managers have.
 */
fun TeamEntity.toUpdatePayload(): JsonObject = JsonObject(
    mapOf(
        "name" to JsonPrimitive(name),
        "description" to (description?.let { JsonPrimitive(it) } ?: JsonNull),
        "avatar_url" to (avatarUrl?.let { JsonPrimitive(it) } ?: JsonNull),
        "cover_url" to (coverUrl?.let { JsonPrimitive(it) } ?: JsonNull),
        "updated_at" to JsonPrimitive(Instant.fromEpochMilliseconds(updatedAt).toString())
    )
)

/** `team_members` row with the embedded `profiles` row (PostgREST resource embedding). */
@Serializable
data class TeamMemberWithProfileDto(
    val id: String,
    @SerialName("team_id") val teamId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("joined_at") val joinedAt: String,
    val priority: Int = 0,
    val profiles: ProfileDto? = null
) {
    fun toMemberEntity(syncStatus: String = "SYNCED") =
        TeamMemberDto(id, teamId, userId, role, joinedAt, priority).toEntity(syncStatus)
}
