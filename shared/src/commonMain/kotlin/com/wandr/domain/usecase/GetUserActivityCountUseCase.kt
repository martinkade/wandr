package com.wandr.domain.usecase

import com.wandr.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow

/** Number of activities the user has recorded or logged. */
class GetUserActivityCountUseCase(private val activityRepository: ActivityRepository) {
    operator fun invoke(userId: String): Flow<Int> = activityRepository.getUserActivityCount(userId)
}
