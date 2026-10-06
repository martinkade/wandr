package com.wandr.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "team_id")
    val teamId: String?,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String?,
    
    @ColumnInfo(name = "activity_type")
    val activityType: String, // hiking, running, cycling
    
    @ColumnInfo(name = "distance_meters")
    val distanceMeters: Double,
    
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Double,
    
    @ColumnInfo(name = "elevation_gain_meters")
    val elevationGainMeters: Double,
    
    @ColumnInfo(name = "fit_file_path")
    val fitFilePath: String?,
    
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    
    @ColumnInfo(name = "end_time")
    val endTime: Long,
    
    @ColumnInfo(name = "is_manual_entry")
    val isManualEntry: Boolean,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    
    @ColumnInfo(name = "show_map", defaultValue = "1")
    val showMap: Boolean = true,

    @ColumnInfo(name = "polyline")
    val polyline: String? = null,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED"
)
