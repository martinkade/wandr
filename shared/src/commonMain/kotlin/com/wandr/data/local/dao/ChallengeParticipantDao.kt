package com.wandr.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.wandr.data.local.entity.ChallengeParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeParticipantDao {
    @Query("SELECT * FROM challenge_participants WHERE challenge_id = :challengeId ORDER BY progress_value DESC")
    fun getParticipantsForChallenge(challengeId: String): Flow<List<ChallengeParticipantEntity>>

    @Query("SELECT * FROM challenge_participants WHERE challenge_id = :challengeId AND team_id = :teamId ORDER BY progress_value DESC")
    fun getParticipantsForChallengeAndTeam(challengeId: String, teamId: String): Flow<List<ChallengeParticipantEntity>>

    @Query("SELECT * FROM challenge_participants WHERE challenge_id = :challengeId AND user_id = :userId")
    suspend fun getParticipant(challengeId: String, userId: String): ChallengeParticipantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ChallengeParticipantEntity)

    @Update
    suspend fun updateParticipant(participant: ChallengeParticipantEntity)
}
