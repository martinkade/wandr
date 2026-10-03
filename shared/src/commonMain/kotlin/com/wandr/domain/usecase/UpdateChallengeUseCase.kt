package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge
import com.wandr.domain.repository.ChallengeRepository

class UpdateChallengeUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(challenge: Challenge): Result<Challenge> {
        challenge.validationError()?.let { return Result.failure(IllegalArgumentException(it)) }
        return challengeRepository.updateChallenge(challenge.normalized())
    }
}
