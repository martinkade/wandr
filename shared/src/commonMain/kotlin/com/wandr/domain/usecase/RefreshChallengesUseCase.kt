package com.wandr.domain.usecase

import com.wandr.domain.repository.ChallengeRepository

class RefreshChallengesUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(): Result<Unit> = challengeRepository.refreshChallenges()
}
