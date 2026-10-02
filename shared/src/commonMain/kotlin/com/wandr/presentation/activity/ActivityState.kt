package com.wandr.presentation.activity

import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint

enum class ActivitySuccess { CREATED, UPDATED, RECORDED, DELETED, MERGED, TRIMMED, DISCARDED }

/**
 * The saved activity overlaps [conflicting] ones in time; nothing is saved until the user chooses a resolution.
 *
 * @param canTrim false when the activity is completely covered by existing ones
 * @param dismissible false for a finished recording: its data would be lost, so the user must pick a resolution
 */
data class ActivityConflict(
    val conflicting: List<Activity>,
    val canTrim: Boolean,
    val dismissible: Boolean
)

/** Activity types offered in the app. */
object ActivityTypes {
    val all = listOf("hiking", "running", "cycling")
}

/** Content of the create / edit form. [activityId] is null while creating. */
data class ActivityForm(
    val activityId: String? = null,
    val title: String = "",
    val description: String = "",
    val activityType: String = "hiking",
    val distanceKm: Double = 5.0,
    val durationMinutes: Double = 45.0,
    val elevationMeters: Double = 0.0,
    /** Epoch milliseconds. */
    val startTime: Long = 0L,
    /** GPS-recorded activities keep their measured values; only title, description and type can change. */
    val isMeasured: Boolean = false
) {
    val isEditing: Boolean get() = activityId != null
}

data class ActivityState(
    val activities: List<Activity> = emptyList(),
    val selectedActivity: Activity? = null,
    /** The recorded track of the selected activity; empty for manual entries and on other devices. */
    val selectedTrack: List<GpsTrackpoint> = emptyList(),
    /** Only the owner of the selected activity may edit it. */
    val canEdit: Boolean = false,

    /** Non-null while the create / edit sheet is open. */
    val form: ActivityForm? = null,

    // Live GPS Tracking Fields
    val trackingActivityType: String = "hiking",
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val liveDistanceMeters: Double = 0.0,
    val liveDurationSeconds: Double = 0.0,
    val liveElevationGainMeters: Double = 0.0,
    val liveTrackpoints: List<GpsTrackpoint> = emptyList(),

    /** Non-null while the conflict resolution wizard is shown. */
    val conflict: ActivityConflict? = null,

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** What succeeded last; the UI maps it to a localized message. Cleared via [ActivityIntent.ClearMessages]. */
    val success: ActivitySuccess? = null
)
