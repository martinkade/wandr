package com.wandr.domain.usecase

import com.wandr.domain.repository.ActivityFeedRepository

class RefreshActivitiesUseCase(private val repository: ActivityFeedRepository) {
    suspend fun team(teamId: String, userId: String) = repository.refreshTeam(teamId, userId)
    suspend fun user(userId: String) = repository.refreshUser(userId)
}
