package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem
import com.wandr.domain.repository.TeamRepository

/** Saves the priority order of the user's teams. Only the first (highest priority) team counts for group challenges. */
class ReorderTeamsUseCase(private val teamRepository: TeamRepository) {
    suspend operator fun invoke(userId: String, orderedTeamIds: List<String>): Result<Unit> {
        if (orderedTeamIds.isEmpty()) return Result.failure(AppError.InvalidInput(InputProblem.NOTHING_TO_ORDER))
        if (orderedTeamIds.toSet().size != orderedTeamIds.size) {
            return Result.failure(AppError.InvalidInput(InputProblem.NOTHING_TO_ORDER))
        }
        return teamRepository.reorderTeams(userId, orderedTeamIds)
    }
}
