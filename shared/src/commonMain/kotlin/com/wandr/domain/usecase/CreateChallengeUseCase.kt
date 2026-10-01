package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge
import com.wandr.domain.repository.ChallengeRepository

class CreateChallengeUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(challenge: Challenge): Result<Challenge> {
        if (challenge.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Challenge title cannot be blank"))
        }
        if (challenge.targetValue <= 0.0) {
            return Result.failure(IllegalArgumentException("Target value must be greater than 0"))
        }
        if (challenge.endDate <= challenge.startDate) {
            return Result.failure(IllegalArgumentException("End date must be after start date"))
        }
        return challengeRepository.createChallenge(challenge)
    }
}
