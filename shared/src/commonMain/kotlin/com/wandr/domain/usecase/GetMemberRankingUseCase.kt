package com.wandr.domain.usecase

import com.wandr.domain.model.LeaderboardEntry
import com.wandr.domain.repository.ChallengeRepository

/**
 * The members' ranking inside a challenge: everybody in an individual challenge (visible to participants and the
 * creator), only the user's own team in a team challenge (other teams only appear as team standings).
 */
class GetMemberRankingUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(challengeId: String): Result<List<LeaderboardEntry>> =
        challengeRepository.getMemberRanking(challengeId)
}
