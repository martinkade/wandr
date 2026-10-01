package com.wandr.domain.usecase

import com.wandr.domain.model.Team
import com.wandr.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow

class GetUserTeamsUseCase(private val teamRepository: TeamRepository) {
    operator fun invoke(userId: String): Flow<List<Team>> {
        return teamRepository.getUserTeams(userId)
    }
}
