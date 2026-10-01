package com.wandr.domain.model

data class Team(
    val id: String,
    val name: String,
    val description: String?,
    val avatarUrl: String?,
    val coverUrl: String?,
    val inviteCode: String,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
)
