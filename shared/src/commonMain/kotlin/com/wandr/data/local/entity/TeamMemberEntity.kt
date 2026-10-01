package com.wandr.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "team_members")
data class TeamMemberEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "team_id")
    val teamId: String,
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "role")
    val role: String, // admin, member
    
    @ColumnInfo(name = "joined_at")
    val joinedAt: Long,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED"
)
