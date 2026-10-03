package com.wandr.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import com.wandr.data.local.entity.TeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getTeamOnce(id: String): TeamEntity?

    @Query("SELECT * FROM teams WHERE id = :id")
    fun getTeamById(id: String): Flow<TeamEntity?>

    /** The user's teams by priority: the first one is the team that counts for group challenges. */
    @Query(
        "SELECT t.* FROM teams t LEFT JOIN team_members m ON m.team_id = t.id AND m.user_id = :userId " +
            "WHERE t.created_by = :userId OR m.user_id IS NOT NULL " +
            "ORDER BY COALESCE(m.priority, 2147483647), t.created_at DESC"
    )
    fun getTeamsForUser(userId: String): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE sync_status = 'DIRTY' OR sync_status = 'PENDING'")
    suspend fun getDirtyTeams(): List<TeamEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity)

    @Update
    suspend fun updateTeam(team: TeamEntity)
}
