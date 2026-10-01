package com.wandr.domain.usecase

import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamImageKind
import com.wandr.domain.repository.TeamRepository

class RemoveTeamImageUseCase(private val teamRepository: TeamRepository) {
    suspend operator fun invoke(teamId: String, kind: TeamImageKind): Result<Team> =
        teamRepository.removeTeamImage(teamId, kind)
}
