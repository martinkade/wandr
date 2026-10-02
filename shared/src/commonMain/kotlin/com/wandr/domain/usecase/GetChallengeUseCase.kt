package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge
import com.wandr.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow

class GetChallengeUseCase(private val challengeRepository: ChallengeRepository) {
    operator fun invoke(challengeId: String): Flow<Challenge?> = challengeRepository.getChallengeById(challengeId)
}
