package com.wandr.domain.usecase

import com.wandr.domain.model.TeamStanding
import com.wandr.domain.repository.ChallengeRepository

/** Team vs. team ranking of a team challenge. */
class GetTeamStandingsUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(challengeId: String): Result<List<TeamStanding>> =
        challengeRepository.getTeamStandings(challengeId)
}
