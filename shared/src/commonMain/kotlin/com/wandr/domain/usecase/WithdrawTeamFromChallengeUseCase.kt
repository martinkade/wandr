package com.wandr.domain.usecase

import com.wandr.domain.repository.ChallengeRepository

/** Takes a team out of a team challenge (team owner / admin only, enforced by the server). */
class WithdrawTeamFromChallengeUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(challengeId: String, teamId: String): Result<Unit> =
        challengeRepository.withdrawTeam(challengeId, teamId)
}
