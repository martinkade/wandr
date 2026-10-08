package com.wandr.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "challenge_participants")
data class ChallengeParticipantEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "challenge_id")
    val challengeId: String,
    
    @ColumnInfo(name = "user_id")
    val userId: String,

    /** The team this participant contributes for in a team challenge; null in an individual challenge. */
    @ColumnInfo(name = "team_id")
    val teamId: String? = null,
    
    @ColumnInfo(name = "progress_value")
    val progressValue: Double = 0.0,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "joined_at")
    val joinedAt: Long,
    
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED"
)
