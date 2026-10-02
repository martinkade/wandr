package com.wandr.domain.usecase

import com.wandr.domain.repository.ChallengeRepository

/** Lets a team (owner / admin only, enforced by the server) take part in a group challenge. */
class EnrollTeamInChallengeUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(challengeId: String, teamId: String, enrolledBy: String): Result<Unit> =
        challengeRepository.enrollTeam(challengeId, teamId, enrolledBy)
}
