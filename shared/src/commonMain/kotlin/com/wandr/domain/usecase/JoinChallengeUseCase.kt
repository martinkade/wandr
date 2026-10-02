package com.wandr.domain.usecase

import com.wandr.domain.repository.ChallengeRepository

/** Joins an individual challenge. Group challenges are joined by enrolling a team instead. */
class JoinChallengeUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(challengeId: String, userId: String): Result<Unit> {
        return challengeRepository.joinChallenge(challengeId, userId)
    }
}
