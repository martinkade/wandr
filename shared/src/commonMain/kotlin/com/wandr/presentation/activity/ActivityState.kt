package com.wandr.presentation.activity

import com.wandr.domain.error.AppError
import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.model.Profile
import com.wandr.domain.model.SocialCounts

/** How good the GPS signal is, derived from the accuracy of the latest fix. */
enum class GpsStatus {
    SEARCHING, WEAK, GOOD;

    companion object {
        /** Fixes less accurate than this (meters) are "weak". */
        const val WEAK_ACCURACY_METERS = 30f

        fun of(accuracyMeters: Float?): GpsStatus = when {
            accuracyMeters == null -> SEARCHING
            accuracyMeters > WEAK_ACCURACY_METERS -> WEAK
            else -> GOOD
        }
    }
}

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
    val isMeasured: Boolean = false,
    /** Privacy: other users may see the route of this activity on the map. */
    val showMap: Boolean = true,
    /** The activity has a route (so the privacy switch makes sense). */
    val hasRoute: Boolean = false
) {
    val isEditing: Boolean get() = activityId != null
}

data class ActivityState(
    val activities: List<Activity> = emptyList(),
    /** The authors of [activities] by user id, for name and avatar on the feed cards. */
    val authors: Map<String, Profile> = emptyMap(),
    /** Likes and comments per activity id, fetched for the whole list in one call. */
    val socialCounts: Map<String, SocialCounts> = emptyMap(),
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
    /** Pace over the last seconds in s/km; null while standing still or without a fix. */
    val liveCurrentPaceSecondsPerKm: Double? = null,
    /** Time in seconds of every completed kilometer, in order. */
    val liveSplitsSeconds: List<Double> = emptyList(),
    /** Accuracy of the latest GPS fix in meters; null while no fix has arrived (searching). */
    val gpsAccuracyMeters: Float? = null,

    /** Non-null while the conflict resolution wizard is shown. */
    val conflict: ActivityConflict? = null,

    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: AppError? = null,
    /** What succeeded last; the UI maps it to a localized message. Cleared via [ActivityIntent.ClearMessages]. */
    val success: ActivitySuccess? = null
) {
    val gpsStatus: GpsStatus get() = GpsStatus.of(gpsAccuracyMeters)
}
