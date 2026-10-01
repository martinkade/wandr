package com.wandr.domain.usecase

import com.wandr.domain.repository.ActivityRepository

class DeleteActivityUseCase(
    private val activityRepository: ActivityRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return activityRepository.deleteActivity(id)
    }
}
