package com.wandr.domain.usecase

import com.wandr.domain.model.Activity
import com.wandr.domain.repository.ActivityRepository

/**
 * Saves changes to an existing activity (local first, synced later). The caller is responsible for only offering this
 * to the activity's owner; the server enforces it as well. A recorded track (`fitFilePath`) is kept as it is.
 */
class UpdateActivityUseCase(private val activityRepository: ActivityRepository) {
    suspend operator fun invoke(activity: Activity): Result<Activity> {
        if (activity.title.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be empty"))
        if (activity.distanceMeters < 0 || activity.durationSeconds < 0 || activity.elevationGainMeters < 0) {
            return Result.failure(IllegalArgumentException("Distance, duration and elevation cannot be negative"))
        }
        return activityRepository.saveActivity(
            activity.copy(title = activity.title.trim(), description = activity.description?.trim()?.ifEmpty { null }),
            trackpoints = null
        )
    }
}
