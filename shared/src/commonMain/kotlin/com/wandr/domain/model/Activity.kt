package com.wandr.domain.model

data class Activity(
    val id: String,
    val userId: String,
    val teamId: String?,
    val title: String,
    val description: String?,
    val activityType: String, // hiking, running, cycling
    val distanceMeters: Double,
    val durationSeconds: Double,
    val elevationGainMeters: Double,
    /** Absolute path of the recorded `.FIT` file on THIS device; null for manual entries or other devices. Never synced. */
    val fitFilePath: String?,
    val startTime: Long,
    val endTime: Long,
    val isManualEntry: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
