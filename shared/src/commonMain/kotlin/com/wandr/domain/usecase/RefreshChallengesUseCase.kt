package com.wandr.domain.usecase

import com.wandr.domain.repository.ChallengeRepository

class RefreshChallengesUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(userId: String): Result<Unit> = challengeRepository.refreshChallenges(userId)
}
