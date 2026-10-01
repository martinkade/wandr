package com.wandr.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "description")
    val description: String?,
    
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    
    @ColumnInfo(name = "cover_url")
    val coverUrl: String?,
    
    @ColumnInfo(name = "invite_code")
    val inviteCode: String,
    
    @ColumnInfo(name = "created_by")
    val createdBy: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED"
)
