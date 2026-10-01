package com.wandr.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.wandr.data.local.entity.ChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges WHERE id = :id")
    fun getChallengeById(id: String): Flow<ChallengeEntity?>

    @Query("SELECT * FROM challenges WHERE team_id = :teamId ORDER BY start_date ASC")
    fun getChallengesForTeam(teamId: String): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE sync_status = 'DIRTY' OR sync_status = 'PENDING'")
    suspend fun getDirtyChallenges(): List<ChallengeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: ChallengeEntity)

    @Update
    suspend fun updateChallenge(challenge: ChallengeEntity)
}
