package com.wandr.domain.usecase

import com.wandr.domain.model.Activity
import com.wandr.domain.model.ConflictResolution
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.repository.ActivityRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class RecordGpsActivityUseCase(
    activityRepository: ActivityRepository
) {
    private val conflictResolver = ActivityConflictResolver(activityRepository)

    /** Fails with `ActivityConflictException` if the recording overlaps other activities and no [resolution] is given. */
    suspend operator fun invoke(
        userId: String,
        teamId: String?,
        title: String,
        description: String?,
        activityType: String,
        distanceMeters: Double,
        durationSeconds: Double,
        elevationGainMeters: Double,
        startTime: Long,
        endTime: Long,
        trackpoints: List<GpsTrackpoint>,
        resolution: ConflictResolution? = null
    ): Result<Activity> {
        val now = Clock.System.now().toEpochMilliseconds()
        val uniqueId = Uuid.random().toString() // the server uses UUID primary keys
        val activity = Activity(
            id = uniqueId,
            userId = userId,
            teamId = teamId,
            title = title,
            description = description,
            activityType = activityType,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            elevationGainMeters = elevationGainMeters,
            fitFilePath = null,
            startTime = startTime,
            endTime = endTime,
            isManualEntry = false,
            createdAt = now,
            updatedAt = now
        )
        return conflictResolver.save(activity, trackpoints = trackpoints, resolution = resolution)
    }
}
