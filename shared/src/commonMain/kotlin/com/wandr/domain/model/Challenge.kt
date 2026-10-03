package com.wandr.domain.model

data class Challenge(
    val id: String,
    val title: String,
    val description: String?,
    val coverUrl: String?,
    val scope: String, // group, individual
    val type: String, // distance, elevation, time
    val targetValue: Double,
    val requireAllMembersCompletion: Boolean = false,
    val startDate: Long,
    val endDate: Long,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** Stored status: true = `active` (published), false = `draft` (only the creator sees it). */
    val isActive: Boolean = true,
    /** Activity types that count for the challenge (e.g. `running`); empty = every type counts. */
    val activityTypes: List<String> = emptyList()
)
