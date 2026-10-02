package com.wandr.domain.usecase

import com.wandr.domain.model.Activity
import com.wandr.domain.model.ConflictResolution
import com.wandr.domain.repository.ActivityRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class CreateManualActivityUseCase(
    activityRepository: ActivityRepository
) {
    private val conflictResolver = ActivityConflictResolver(activityRepository)

    /** Fails with `ActivityConflictException` if the time range overlaps other activities and no [resolution] is given. */
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
        resolution: ConflictResolution? = null
    ): Result<Activity> {
        if (title.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be empty"))
        if (distanceMeters < 0 || durationSeconds < 0 || elevationGainMeters < 0) {
            return Result.failure(IllegalArgumentException("Distance, duration and elevation cannot be negative"))
        }
        val now = Clock.System.now().toEpochMilliseconds()
        val uniqueId = Uuid.random().toString() // the server uses UUID primary keys
        val activity = Activity(
            id = uniqueId,
            userId = userId,
            teamId = teamId,
            title = title.trim(),
            description = description?.trim()?.ifEmpty { null },
            activityType = activityType,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            elevationGainMeters = elevationGainMeters,
            fitFilePath = null,
            startTime = startTime,
            endTime = startTime + (durationSeconds * 1000).toLong(),
            isManualEntry = true,
            createdAt = now,
            updatedAt = now
        )
        return conflictResolver.save(activity, trackpoints = null, resolution = resolution)
    }
}
