package com.wandr.domain.usecase

import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.repository.ActivityRepository

/** The recorded GPS track of an activity; only available on the device that recorded it. */
class GetActivityTrackUseCase(private val activityRepository: ActivityRepository) {
    suspend operator fun invoke(activityId: String): List<GpsTrackpoint> = activityRepository.getTrackpoints(activityId)
}
