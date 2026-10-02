package com.wandr.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String?,
    
    @ColumnInfo(name = "cover_url")
    val coverUrl: String?,
    
    @ColumnInfo(name = "scope")
    val scope: String, // group, individual
    
    @ColumnInfo(name = "type")
    val type: String, // distance, elevation, time
    
    @ColumnInfo(name = "target_value")
    val targetValue: Double,
    
    @ColumnInfo(name = "require_all_members_completion")
    val requireAllMembersCompletion: Boolean = false,
    
    @ColumnInfo(name = "start_date")
    val startDate: Long,
    
    @ColumnInfo(name = "end_date")
    val endDate: Long,
    
    @ColumnInfo(name = "status")
    val status: String, // stored: draft or active only; completed / expired are derived at runtime
    
    @ColumnInfo(name = "created_by")
    val createdBy: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED"
)
