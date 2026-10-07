package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem
import com.wandr.domain.model.Team
import com.wandr.domain.repository.TeamRepository

class CreateTeamUseCase(private val teamRepository: TeamRepository) {
    suspend operator fun invoke(name: String, description: String?, creatorId: String): Result<Team> {
        if (name.isBlank()) {
            return Result.failure(AppError.InvalidInput(InputProblem.NAME_REQUIRED))
        }
        return teamRepository.createTeam(name.trim(), description?.trim(), creatorId)
    }
}
