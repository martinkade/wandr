package com.wandr.domain.usecase

import com.wandr.domain.model.Team
import com.wandr.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow

class GetTeamUseCase(private val teamRepository: TeamRepository) {
    operator fun invoke(teamId: String): Flow<Team?> = teamRepository.getTeamById(teamId)
}
