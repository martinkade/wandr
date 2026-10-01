package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge
import com.wandr.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow

class GetTeamChallengesUseCase(private val challengeRepository: ChallengeRepository) {
    operator fun invoke(teamId: String): Flow<List<Challenge>> {
        return challengeRepository.getTeamChallenges(teamId)
    }
}
