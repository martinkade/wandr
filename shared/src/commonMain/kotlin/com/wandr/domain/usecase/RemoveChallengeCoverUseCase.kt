package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge
import com.wandr.domain.repository.ChallengeRepository

class RemoveChallengeCoverUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(challengeId: String): Result<Challenge> =
        challengeRepository.removeChallengeCover(challengeId)
}
