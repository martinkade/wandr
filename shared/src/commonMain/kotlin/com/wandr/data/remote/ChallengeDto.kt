package com.wandr.data.remote

import com.wandr.data.local.entity.ChallengeEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Instant

/** Row of `public.challenges`; timestamps are ISO-8601 `TIMESTAMPTZ` strings. */
@Serializable
data class ChallengeDto(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    val scope: String,
    val type: String,
    @SerialName("target_value") val targetValue: Double,
    @SerialName("require_all_members_completion") val requireAllMembersCompletion: Boolean = false,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val status: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
) {
    fun toEntity(syncStatus: String = "SYNCED") = ChallengeEntity(
        id = id,
        title = title,
        description = description,
        coverUrl = coverUrl,
        scope = scope,
        type = type,
        targetValue = targetValue,
        requireAllMembersCompletion = requireAllMembersCompletion,
        startDate = Instant.parse(startDate).toEpochMilliseconds(),
        endDate = Instant.parse(endDate).toEpochMilliseconds(),
        status = status,
        createdBy = createdBy,
        createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
        updatedAt = Instant.parse(updatedAt).toEpochMilliseconds(),
        syncStatus = syncStatus
    )
}

fun ChallengeEntity.toDto() = ChallengeDto(
    id = id,
    title = title,
    description = description,
    coverUrl = coverUrl,
    scope = scope,
    type = type,
    targetValue = targetValue,
    requireAllMembersCompletion = requireAllMembersCompletion,
    startDate = Instant.fromEpochMilliseconds(startDate).toString(),
    endDate = Instant.fromEpochMilliseconds(endDate).toString(),
    status = status,
    createdBy = createdBy,
    createdAt = Instant.fromEpochMilliseconds(createdAt).toString(),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt).toString()
)

/**
 * Payload for `UPDATE public.challenges`. Built as a [JsonObject] so cleared values are sent as explicit JSON
 * `null`. Never an upsert: that would additionally need the INSERT policy.
 */
fun ChallengeEntity.toUpdatePayload(): JsonObject = JsonObject(
    mapOf(
        "title" to JsonPrimitive(title),
        "description" to (description?.let { JsonPrimitive(it) } ?: JsonNull),
        "cover_url" to (coverUrl?.let { JsonPrimitive(it) } ?: JsonNull),
        "scope" to JsonPrimitive(scope),
        "type" to JsonPrimitive(type),
        "target_value" to JsonPrimitive(targetValue),
        "require_all_members_completion" to JsonPrimitive(requireAllMembersCompletion),
        "start_date" to JsonPrimitive(Instant.fromEpochMilliseconds(startDate).toString()),
        "end_date" to JsonPrimitive(Instant.fromEpochMilliseconds(endDate).toString()),
        "status" to JsonPrimitive(status),
        "updated_at" to JsonPrimitive(Instant.fromEpochMilliseconds(updatedAt).toString())
    )
)
