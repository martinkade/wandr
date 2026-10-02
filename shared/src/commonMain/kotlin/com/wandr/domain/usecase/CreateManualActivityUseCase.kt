package com.wandr.domain.usecase

import com.wandr.domain.model.Activity
import com.wandr.domain.repository.ActivityRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class CreateManualActivityUseCase(
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(
        userId: String,
        teamId: String?,
        title: String,
        description: String?,
        activityType: String,
        distanceMeters: Double,
        durationSeconds: Double,
        elevationGainMeters: Double,
        startTime: Long
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
            endTime = startTime + (durationSeconds * 1000).toLong(),
            isManualEntry = true,
            createdAt = now,
            updatedAt = now
        )
        return activityRepository.saveActivity(activity, trackpoints = null)
    }
}
