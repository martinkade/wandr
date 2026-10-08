package com.wandr.domain.usecase

import com.wandr.domain.model.Team
import com.wandr.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow

/** The teams the user owns or administers; only they may enroll a team in a team challenge or withdraw it. */
class GetAdminTeamsUseCase(private val teamRepository: TeamRepository) {
    operator fun invoke(userId: String): Flow<List<Team>> = teamRepository.getAdminTeams(userId)
}
