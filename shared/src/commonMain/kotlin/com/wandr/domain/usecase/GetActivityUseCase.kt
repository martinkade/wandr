package com.wandr.domain.usecase

import com.wandr.domain.model.Activity
import com.wandr.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow

class GetActivityUseCase(private val activityRepository: ActivityRepository) {
    operator fun invoke(activityId: String): Flow<Activity?> = activityRepository.getActivityById(activityId)
}
