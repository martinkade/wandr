package com.wandr.domain.usecase

import com.wandr.domain.model.Activity
import com.wandr.domain.model.ConflictResolution
import com.wandr.domain.repository.ActivityRepository

/**
 * Saves changes to an existing activity (local first, synced later). The caller is responsible for only offering this
 * to the activity's owner; the server enforces it as well. A recorded track (`fitFilePath`) is kept as it is.
 */
class UpdateActivityUseCase(private val activityRepository: ActivityRepository) {
    private val conflictResolver = ActivityConflictResolver(activityRepository)

    /**
     * @param checkConflicts false when the time range was not changed, so already existing overlaps do not block
     * edits of title, description etc.
     * @param resolution how to resolve an overlap; without one an `ActivityConflictException` is returned
     */
    suspend operator fun invoke(
        activity: Activity,
        checkConflicts: Boolean = true,
        resolution: ConflictResolution? = null
    ): Result<Activity> {
        if (activity.title.isBlank()) return Result.failure(IllegalArgumentException("Title cannot be empty"))
        if (activity.distanceMeters < 0 || activity.durationSeconds < 0 || activity.elevationGainMeters < 0) {
            return Result.failure(IllegalArgumentException("Distance, duration and elevation cannot be negative"))
        }
        val cleaned = activity.copy(title = activity.title.trim(), description = activity.description?.trim()?.ifEmpty { null })
        return if (checkConflicts) {
            conflictResolver.save(cleaned, trackpoints = null, resolution = resolution)
        } else {
            activityRepository.saveActivity(cleaned, trackpoints = null)
        }
    }
}
