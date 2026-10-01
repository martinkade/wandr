package com.wandr.domain.model

data class Profile(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val systemRole: String = "user",
    val createdAt: Long,
    val updatedAt: Long
)
