package com.wandr.domain.usecase

import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.repository.ActivityRepository
import kotlin.time.Clock
import kotlin.random.Random

class RecordGpsActivityUseCase(
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
        startTime: Long,
        endTime: Long,
        trackpoints: List<GpsTrackpoint>
    ): Result<Activity> {
        val now = Clock.System.now().toEpochMilliseconds()
        val uniqueId = "act_gps_${now}_${Random.nextInt(100000, 999999)}"
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
        return activityRepository.saveActivity(activity, trackpoints = trackpoints)
    }
}
