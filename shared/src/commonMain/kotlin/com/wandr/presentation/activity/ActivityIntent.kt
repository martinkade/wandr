package com.wandr.presentation.activity

import com.wandr.domain.model.ConflictResolution
import com.wandr.domain.model.GpsTrackpoint

sealed interface ActivityIntent {
    data class LoadUserActivities(val userId: String) : ActivityIntent
    data class LoadTeamActivities(val teamId: String) : ActivityIntent

    /** Loads one activity (and its local GPS track) for the detail screen. */
    data class SelectActivity(val activityId: String) : ActivityIntent

    /** Opens an empty manual-entry form. */
    data object StartCreate : ActivityIntent

    /** Opens the form filled with an existing activity; only its owner may do this. */
    data class StartEdit(val activityId: String) : ActivityIntent
    data class TitleChanged(val title: String) : ActivityIntent
    data class DescriptionChanged(val description: String) : ActivityIntent
    data class TypeChanged(val type: String) : ActivityIntent
    data class DistanceChanged(val km: Double) : ActivityIntent
    data class DurationChanged(val minutes: Double) : ActivityIntent
    data class ElevationChanged(val meters: Double) : ActivityIntent
    data class StartTimeChanged(val millis: Long) : ActivityIntent

    /** Creates or updates, depending on the form. [teamId] only applies to new activities. */
    data class SubmitForm(val userId: String, val teamId: String?) : ActivityIntent

    /** Closes the form and drops its content. */
    data object DiscardForm : ActivityIntent

    data class StartGpsTracking(val activityType: String) : ActivityIntent
    data class AddTrackpoint(val trackpoint: GpsTrackpoint) : ActivityIntent
    object PauseGpsTracking : ActivityIntent
    object ResumeGpsTracking : ActivityIntent
    data class StopAndSaveGpsTracking(val userId: String, val teamId: String?, val title: String) : ActivityIntent

    data class ResolveConflict(val resolution: ConflictResolution) : ActivityIntent
    object DismissConflict : ActivityIntent
    data class DeleteActivity(val activityId: String) : ActivityIntent
    object ClearMessages : ActivityIntent
}
