package com.wandr.domain.usecase

import com.wandr.domain.model.Activity
import com.wandr.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow

class GetUserActivitiesUseCase(
    private val activityRepository: ActivityRepository
) {
    operator fun invoke(userId: String): Flow<List<Activity>> {
        return activityRepository.getUserActivities(userId)
    }
}
