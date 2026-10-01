package com.wandr.domain.usecase

import com.wandr.domain.repository.TeamRepository

class RefreshUserTeamsUseCase(private val teamRepository: TeamRepository) {
    suspend operator fun invoke(userId: String): Result<Unit> = teamRepository.refreshUserTeams(userId)
}
