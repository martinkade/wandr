package com.wandr.domain.usecase

import com.wandr.domain.repository.ActivityFeedRepository

class RefreshActivitiesUseCase(private val repository: ActivityFeedRepository) {
    suspend fun team(teamId: String) = repository.refreshTeam(teamId)
    suspend fun user(userId: String) = repository.refreshUser(userId)
}
