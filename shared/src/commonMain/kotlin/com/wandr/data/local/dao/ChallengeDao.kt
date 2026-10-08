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

    @Query("SELECT * FROM challenges WHERE id = :id")
    suspend fun getChallengeOnce(id: String): ChallengeEntity?

    /** All challenges are visible to every signed-in user (individual ones and team ones teams can enroll in). */
    @Query("SELECT * FROM challenges ORDER BY start_date ASC")
    fun getAllChallenges(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges")
    suspend fun getAllChallengesOnce(): List<ChallengeEntity>

    @Query("DELETE FROM challenges WHERE id = :id")
    suspend fun deleteChallenge(id: String)

    @Query("SELECT * FROM challenges WHERE sync_status = 'DIRTY' OR sync_status = 'PENDING'")
    suspend fun getDirtyChallenges(): List<ChallengeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: ChallengeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenges(challenges: List<ChallengeEntity>)

    @Update
    suspend fun updateChallenge(challenge: ChallengeEntity)
}
