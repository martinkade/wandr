package com.wandr.domain.usecase

import com.wandr.domain.model.ChallengeParticipation
import com.wandr.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow

/** The challenges the user currently takes part in (individually or through an enrolled team). */
class GetChallengeParticipationsUseCase(private val challengeRepository: ChallengeRepository) {
    operator fun invoke(userId: String): Flow<List<ChallengeParticipation>> = challengeRepository.getParticipations(userId)
}
