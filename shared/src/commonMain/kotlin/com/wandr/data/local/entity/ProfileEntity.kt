package com.wandr.data.local.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.wandr.domain.model.SystemRole

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "username")
    val username: String,
    
    @ColumnInfo(name = "display_name")
    val displayName: String,
    
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    
    @ColumnInfo(name = "bio")
    val bio: String?,
    
    @ColumnInfo(name = "system_role")
    val systemRole: SystemRole = SystemRole.USER,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "SYNCED" // PENDING, DIRTY, SYNCED
)
