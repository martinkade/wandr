package com.wandr.domain.usecase

import com.wandr.domain.repository.TeamRepository

class RefreshTeamDetailsUseCase(private val teamRepository: TeamRepository) {
    suspend operator fun invoke(teamId: String): Result<Unit> = teamRepository.refreshTeamDetails(teamId)
}
