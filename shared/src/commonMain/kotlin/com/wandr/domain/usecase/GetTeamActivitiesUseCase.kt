package com.wandr.domain.usecase

import com.wandr.domain.model.Activity
import com.wandr.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow

class GetTeamActivitiesUseCase(
    private val activityRepository: ActivityRepository
) {
    operator fun invoke(teamId: String): Flow<List<Activity>> {
        return activityRepository.getTeamActivities(teamId)
    }
}
