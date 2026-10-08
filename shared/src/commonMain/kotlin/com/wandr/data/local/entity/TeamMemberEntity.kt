package com.wandr.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.wandr.domain.model.TeamRole

@Entity(tableName = "team_members")
data class TeamMemberEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "team_id")
    val teamId: String,
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "role")
    val role: TeamRole,
    
    @ColumnInfo(name = "joined_at")
    val joinedAt: Long,

    /** Order of the user's teams, 0 = highest priority. Only the highest-priority team contributes to team challenges. */
    @ColumnInfo(name = "priority", defaultValue = "0")
    val priority: Int = 0,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED"
)
