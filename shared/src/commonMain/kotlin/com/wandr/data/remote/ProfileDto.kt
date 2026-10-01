package com.wandr.data.remote

import com.wandr.data.local.entity.ProfileEntity
import com.wandr.domain.model.SystemRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Instant

/** Row of `public.profiles`; timestamps are ISO-8601 `TIMESTAMPTZ` strings. */
@Serializable
data class ProfileDto(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("system_role") val systemRole: String = "user",
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
) {
    fun toEntity(syncStatus: String = "SYNCED") = ProfileEntity(
        id = id,
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        bio = bio,
        systemRole = SystemRole.fromValue(systemRole),
        createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
        updatedAt = Instant.parse(updatedAt).toEpochMilliseconds(),
        syncStatus = syncStatus
    )
}

/**
 * Payload for `UPDATE public.profiles`. Built as a [JsonObject] so that `avatar_url`/`bio` are sent as
 * explicit JSON `null` when cleared (the Supabase serializer omits null properties of data classes).
 * `profiles` has no INSERT policy (rows are created by the signup trigger), so this is never an upsert.
 */
fun ProfileEntity.toUpdatePayload(): JsonObject = JsonObject(
    mapOf(
        "display_name" to JsonPrimitive(displayName),
        "bio" to (bio?.let { JsonPrimitive(it) } ?: JsonNull),
        "avatar_url" to (avatarUrl?.let { JsonPrimitive(it) } ?: JsonNull),
        "updated_at" to JsonPrimitive(Instant.fromEpochMilliseconds(updatedAt).toString())
    )
)
