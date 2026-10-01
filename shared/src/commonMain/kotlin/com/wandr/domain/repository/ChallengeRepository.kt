package com.wandr.domain.repository

import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeParticipant
import com.wandr.domain.model.LeaderboardEntry
import kotlinx.coroutines.flow.Flow

interface ChallengeRepository {
    fun getChallengeById(challengeId: String): Flow<Challenge?>
    fun getTeamChallenges(teamId: String): Flow<List<Challenge>>
    fun getChallengeParticipants(challengeId: String): Flow<List<ChallengeParticipant>>
    fun getPrivacyFirstLeaderboard(challengeId: String, teamId: String): Flow<List<LeaderboardEntry>>
    suspend fun createChallenge(challenge: Challenge): Result<Challenge>
    suspend fun uploadChallengeCover(challengeId: String, bytes: ByteArray, fileName: String): Result<String>
    suspend fun joinChallenge(challengeId: String, userId: String): Result<Unit>
    suspend fun updateParticipantProgress(challengeId: String, userId: String, additionalProgress: Double): Result<Unit>
}
