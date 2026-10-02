package com.wandr.domain.repository

import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeParticipant
import com.wandr.domain.model.LeaderboardEntry
import com.wandr.domain.model.TeamStanding
import kotlinx.coroutines.flow.Flow

interface ChallengeRepository {
    fun getChallengeById(challengeId: String): Flow<Challenge?>

    /** Every challenge visible to the user: open (individual) ones and group ones teams can enroll in. */
    fun getChallenges(): Flow<List<Challenge>>

    /** Pulls the challenges visible to the signed-in user (RLS-filtered) into the local cache. */
    suspend fun refreshChallenges(): Result<Unit>

    fun getChallengeParticipants(challengeId: String): Flow<List<ChallengeParticipant>>

    /** Progress of the members of one team (visible to that team only) in a group challenge. */
    fun getTeamContributions(challengeId: String, teamId: String): Flow<List<LeaderboardEntry>>

    /** Team vs. team ranking of a group challenge; aggregates only, computed on the server. */
    suspend fun getTeamStandings(challengeId: String): Result<List<TeamStanding>>

    suspend fun createChallenge(challenge: Challenge): Result<Challenge>

    /** Saves locally first; if the push fails the challenge stays queued for the sync manager. */
    suspend fun updateChallenge(challenge: Challenge): Result<Challenge>
    suspend fun uploadChallengeCover(challengeId: String, bytes: ByteArray, fileName: String): Result<String>

    /** Joins an individual challenge for [userId]. Group challenges are joined by enrolling a team. */
    suspend fun joinChallenge(challengeId: String, userId: String): Result<Unit>

    /** Enrolls [teamId] in a group challenge; all its members then contribute to the team's result. */
    suspend fun enrollTeam(challengeId: String, teamId: String, enrolledBy: String): Result<Unit>
    suspend fun updateParticipantProgress(challengeId: String, userId: String, additionalProgress: Double): Result<Unit>
}
