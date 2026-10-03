package com.wandr.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.wandr.data.local.entity.TeamMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamMemberDao {
    @Query("SELECT * FROM team_members WHERE team_id = :teamId")
    fun getMembersForTeam(teamId: String): Flow<List<TeamMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: TeamMemberEntity)

    @Query("DELETE FROM team_members WHERE team_id = :teamId")
    suspend fun deleteMembersForTeam(teamId: String)

    @Query("DELETE FROM team_members WHERE team_id = :teamId AND user_id = :userId")
    suspend fun deleteMember(teamId: String, userId: String)

    @Query("UPDATE team_members SET priority = :priority WHERE team_id = :teamId AND user_id = :userId")
    suspend fun setPriority(teamId: String, userId: String, priority: Int)

    /** The lowest priority (highest number) the user has, -1 without memberships. */
    @Query("SELECT COALESCE(MAX(priority), -1) FROM team_members WHERE user_id = :userId")
    suspend fun maxPriority(userId: String): Int
}
