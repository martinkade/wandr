package com.wandr.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.wandr.data.local.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE user_id = :userId AND sync_status != 'DELETED' ORDER BY start_time DESC")
    fun getActivitiesForUser(userId: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id AND sync_status != 'DELETED'")
    fun getActivityById(id: String): Flow<ActivityEntity?>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityOnce(id: String): ActivityEntity?

    @Query("SELECT COUNT(*) FROM activities WHERE user_id = :userId AND sync_status != 'DELETED'")
    fun getActivityCountForUser(userId: String): Flow<Int>

    @Query("SELECT * FROM activities WHERE team_id = :teamId AND sync_status != 'DELETED' ORDER BY start_time DESC")
    fun getActivitiesForTeam(teamId: String): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE user_id = :userId AND sync_status != 'DELETED' AND start_time < :endTime AND end_time > :startTime")
    suspend fun getOverlappingActivities(userId: String, startTime: Long, endTime: Long): List<ActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivity(id: String)

    @Query("SELECT * FROM activities WHERE sync_status = 'DIRTY' OR sync_status = 'PENDING'")
    suspend fun getDirtyActivities(): List<ActivityEntity>

    /**
     * Deleting is a two-step affair: the row is only marked, so the sync engine can delete it on the server too (a plain
     * delete would let the next pull bring it back). It disappears from all queries above immediately.
     */
    @Query("UPDATE activities SET sync_status = 'DELETED' WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("SELECT * FROM activities WHERE sync_status = 'DELETED'")
    suspend fun getDeletedActivities(): List<ActivityEntity>
}
