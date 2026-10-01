package com.wandr.domain.usecase

import com.wandr.domain.repository.ChallengeRepository

class JoinChallengeUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(challengeId: String, userId: String): Result<Unit> {
        return challengeRepository.joinChallenge(challengeId, userId)
    }
}
