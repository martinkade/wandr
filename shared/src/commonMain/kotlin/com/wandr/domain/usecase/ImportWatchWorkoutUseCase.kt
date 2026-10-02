package com.wandr.domain.usecase

import com.wandr.domain.model.Activity
import com.wandr.domain.model.ConflictResolution
import com.wandr.domain.repository.ActivityRepository
import com.wandr.domain.watch.WatchWorkout
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/**
 * Turns a watch workout into an [Activity] (recorded, not manual) of [userId]. Importing the same workout again is a
 * no-op. A time overlap with other activities fails with `ActivityConflictException` unless a [ConflictResolution]
 * is given (see [ActivityConflictResolver]).
 */
class ImportWatchWorkoutUseCase(private val activityRepository: ActivityRepository) {
    private val conflictResolver = ActivityConflictResolver(activityRepository)

    suspend operator fun invoke(
        userId: String,
        teamId: String?,
        workout: WatchWorkout,
        resolution: ConflictResolution? = null
    ): Result<Activity> {
        activityRepository.getActivityById(workout.id).first()?.let { return Result.success(it) }

        val now = Clock.System.now().toEpochMilliseconds()
        val activity = Activity(
            id = workout.id,
            userId = userId,
            teamId = teamId,
            title = "${workout.activityType.replaceFirstChar { it.uppercase() }} Workout",
            description = "Recorded on watch",
            activityType = workout.activityType,
            distanceMeters = workout.distanceMeters,
            durationSeconds = (workout.endTime - workout.startTime) / 1000.0,
            elevationGainMeters = workout.elevationGainMeters,
            fitFilePath = null,
            startTime = workout.startTime,
            endTime = workout.endTime,
            isManualEntry = false,
            createdAt = now,
            updatedAt = now
        )
        return conflictResolver.save(activity, workout.trackpoints.map { it.toGpsTrackpoint() }, resolution)
    }
}
