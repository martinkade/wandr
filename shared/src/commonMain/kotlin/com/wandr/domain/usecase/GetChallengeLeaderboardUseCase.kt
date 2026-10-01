package com.wandr.domain.usecase

import com.wandr.domain.model.LeaderboardEntry
import com.wandr.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow

class GetChallengeLeaderboardUseCase(private val challengeRepository: ChallengeRepository) {
    operator fun invoke(challengeId: String, teamId: String): Flow<List<LeaderboardEntry>> {
        return challengeRepository.getPrivacyFirstLeaderboard(challengeId, teamId)
    }
}
