package com.wandr.presentation.activity

import com.wandr.domain.model.GpsTrackpoint

sealed interface ActivityIntent {
    data class LoadUserActivities(val userId: String) : ActivityIntent
    data class LoadTeamActivities(val teamId: String) : ActivityIntent
    
    data class UpdateManualTitle(val title: String) : ActivityIntent
    data class UpdateManualDescription(val description: String) : ActivityIntent
    data class UpdateManualActivityType(val type: String) : ActivityIntent
    data class UpdateManualDistance(val km: Double) : ActivityIntent
    data class UpdateManualDuration(val minutes: Double) : ActivityIntent
    data class UpdateManualElevation(val meters: Double) : ActivityIntent
    
    data class SubmitManualActivity(val userId: String, val teamId: String?) : ActivityIntent
    
    data class StartGpsTracking(val activityType: String) : ActivityIntent
    data class AddTrackpoint(val trackpoint: GpsTrackpoint) : ActivityIntent
    object PauseGpsTracking : ActivityIntent
    object ResumeGpsTracking : ActivityIntent
    data class StopAndSaveGpsTracking(val userId: String, val teamId: String?, val title: String) : ActivityIntent
    
    data class DeleteActivity(val activityId: String) : ActivityIntent
    object ClearMessages : ActivityIntent
}
