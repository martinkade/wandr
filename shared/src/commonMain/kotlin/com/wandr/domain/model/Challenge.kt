package com.wandr.domain.model

data class Challenge(
    val id: String,
    val teamId: String?,
    val title: String,
    val description: String?,
    val coverUrl: String?,
    val scope: String, // group, individual
    val type: String, // distance, elevation, time
    val targetValue: Double,
    val requireAllMembersCompletion: Boolean = false,
    val startDate: Long,
    val endDate: Long,
    val status: String, // planned, active, completed, expired
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
)
