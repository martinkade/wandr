package com.wandr.presentation.activity

import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint

data class ActivityState(
    val activities: List<Activity> = emptyList(),
    val selectedActivity: Activity? = null,
    
    // Manual Entry Fields
    val manualTitle: String = "",
    val manualDescription: String = "",
    val manualActivityType: String = "hiking", // hiking, running, cycling
    val manualDistanceKm: Double = 5.0,
    val manualDurationMinutes: Double = 45.0,
    val manualElevationMeters: Double = 120.0,
    
    // Live GPS Tracking Fields
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val liveDistanceMeters: Double = 0.0,
    val liveDurationSeconds: Double = 0.0,
    val liveElevationGainMeters: Double = 0.0,
    val liveTrackpoints: List<GpsTrackpoint> = emptyList(),
    
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
