package com.wandr.data.remote

import com.wandr.data.local.entity.ActivityEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Row of `public.activities`. Deliberately has **no FIT file field**: the recorded `.FIT` file stays on the device
 * that created it (see `FitFileStorage`), so its local path must never be sent to the server.
 */
@Serializable
data class ActivityDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("team_id") val teamId: String? = null,
    val title: String,
    val description: String? = null,
    @SerialName("activity_type") val activityType: String,
    @SerialName("distance_meters") val distanceMeters: Double,
    @SerialName("duration_seconds") val durationSeconds: Double,
    @SerialName("elevation_gain_meters") val elevationGainMeters: Double,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String,
    @SerialName("is_manual_entry") val isManualEntry: Boolean,
    @SerialName("show_map") val showMap: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

fun ActivityEntity.toDto() = ActivityDto(
    id = id,
    userId = userId,
    teamId = teamId,
    title = title,
    description = description,
    activityType = activityType,
    distanceMeters = distanceMeters,
    durationSeconds = durationSeconds,
    elevationGainMeters = elevationGainMeters,
    startTime = Instant.fromEpochMilliseconds(startTime).toString(),
    endTime = Instant.fromEpochMilliseconds(endTime).toString(),
    isManualEntry = isManualEntry,
    showMap = showMap,
    createdAt = Instant.fromEpochMilliseconds(createdAt).toString(),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt).toString()
)

/** [fitFilePath] stays whatever this device already has; the server never knows it. */
fun ActivityDto.toEntity(fitFilePath: String? = null, polyline: String? = null, syncStatus: String = "SYNCED") = ActivityEntity(
    id = id,
    userId = userId,
    teamId = teamId,
    title = title,
    description = description,
    activityType = activityType,
    distanceMeters = distanceMeters,
    durationSeconds = durationSeconds,
    elevationGainMeters = elevationGainMeters,
    fitFilePath = fitFilePath,
    startTime = Instant.parse(startTime).toEpochMilliseconds(),
    endTime = Instant.parse(endTime).toEpochMilliseconds(),
    isManualEntry = isManualEntry,
    showMap = showMap,
    polyline = polyline,
    createdAt = Instant.parse(createdAt).toEpochMilliseconds(),
    updatedAt = Instant.parse(updatedAt).toEpochMilliseconds(),
    syncStatus = syncStatus
)

/** Row of `public.activity_routes`: the route is stored apart from the activity so the server can hide it (map privacy). */
@Serializable
data class ActivityRouteDto(
    @SerialName("activity_id") val activityId: String,
    @SerialName("user_id") val userId: String,
    val polyline: String
)
