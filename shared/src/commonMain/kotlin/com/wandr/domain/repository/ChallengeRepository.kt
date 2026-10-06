package com.wandr.domain.repository

import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeParticipant
import com.wandr.domain.model.ChallengeParticipation
import com.wandr.domain.model.LeaderboardEntry
import com.wandr.domain.model.TeamStanding
import kotlinx.coroutines.flow.Flow

interface ChallengeRepository {
    fun getChallengeById(challengeId: String): Flow<Challenge?>

    /** Every challenge visible to the user: open (individual) ones and group ones teams can enroll in. */
    fun getChallenges(): Flow<List<Challenge>>

    /**
     * Pulls the challenges visible to the signed-in user (RLS-filtered) and [userId]'s own participations into the
     * local cache.
     */
    suspend fun refreshChallenges(userId: String): Result<Unit>

    /** The challenges [userId] takes part in. */
    fun getParticipations(userId: String): Flow<List<ChallengeParticipation>>

    fun getChallengeParticipants(challengeId: String): Flow<List<ChallengeParticipant>>

    /** The ranking of the members in the challenge, from the server (it decides who may see whom). */
    suspend fun getMemberRanking(challengeId: String): Result<List<LeaderboardEntry>>

    /** Team vs. team ranking of a group challenge; aggregates only, computed on the server. */
    suspend fun getTeamStandings(challengeId: String): Result<List<TeamStanding>>

    suspend fun createChallenge(challenge: Challenge): Result<Challenge>

    /** Saves locally first; if the push fails the challenge stays queued for the sync manager. */
    suspend fun updateChallenge(challenge: Challenge): Result<Challenge>

    /** Uploads [jpegBytes] as the cover, links it to the challenge and removes earlier covers. */
    suspend fun setChallengeCover(challengeId: String, jpegBytes: ByteArray): Result<Challenge>

    /** Clears the cover and deletes the stored cover files. */
    suspend fun removeChallengeCover(challengeId: String): Result<Challenge>


    /** Joins an individual challenge for [userId]. Group challenges are joined by enrolling a team. */
    suspend fun joinChallenge(challengeId: String, userId: String): Result<Unit>

    /** Leaves an individual challenge. */
    suspend fun leaveChallenge(challengeId: String, userId: String): Result<Unit>

    /** Withdraws [teamId] from a group challenge (team owner / admin only, enforced by the server). */
    suspend fun withdrawTeam(challengeId: String, teamId: String): Result<Unit>

    /** Enrolls [teamId] in a group challenge; all its members then contribute to the team's result. */
    suspend fun enrollTeam(challengeId: String, teamId: String, enrolledBy: String): Result<Unit>
}
