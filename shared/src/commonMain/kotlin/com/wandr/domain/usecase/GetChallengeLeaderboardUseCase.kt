package com.wandr.domain.usecase

import com.wandr.domain.model.LeaderboardEntry
import com.wandr.domain.repository.ChallengeRepository
import kotlinx.coroutines.flow.Flow

/** Progress of the members of [teamId] in a group challenge; only visible to that team. */
class GetChallengeLeaderboardUseCase(private val challengeRepository: ChallengeRepository) {
    operator fun invoke(challengeId: String, teamId: String): Flow<List<LeaderboardEntry>> {
        return challengeRepository.getTeamContributions(challengeId, teamId)
    }
}
