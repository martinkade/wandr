package com.wandr.domain.usecase

import com.wandr.domain.model.Team
import com.wandr.domain.repository.TeamRepository

class UpdateTeamUseCase(private val teamRepository: TeamRepository) {
    suspend operator fun invoke(team: Team): Result<Team> {
        val name = team.name.trim()
        if (name.isEmpty()) {
            return Result.failure(IllegalArgumentException("Team name cannot be empty"))
        }
        return teamRepository.updateTeam(team.copy(name = name, description = team.description?.trim()))
    }
}
