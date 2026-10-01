package com.wandr.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wandr.data.local.entity.ChallengeParticipantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeParticipantDao {
    @Query("SELECT * FROM challenge_participants WHERE challenge_id = :challengeId ORDER BY progress_value DESC")
    fun getParticipantsForChallenge(challengeId: String): Flow<List<ChallengeParticipantEntity>>

    @Query("SELECT * FROM challenge_participants WHERE challenge_id = :challengeId AND user_id = :userId")
    suspend fun getParticipant(challengeId: String, userId: String): ChallengeParticipantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ChallengeParticipantEntity)

    @Update
    suspend fun updateParticipant(participant: ChallengeParticipantEntity)
}
