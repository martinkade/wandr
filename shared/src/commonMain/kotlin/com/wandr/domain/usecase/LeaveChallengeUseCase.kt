package com.wandr.domain.usecase

import com.wandr.domain.repository.ChallengeRepository

/** Leaves an individual challenge. */
class LeaveChallengeUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(challengeId: String, userId: String): Result<Unit> =
        challengeRepository.leaveChallenge(challengeId, userId)
}
