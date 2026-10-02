package com.wandr.domain.model

/** How the user resolves a time overlap between a new/edited activity and activities that already exist. */
enum class ConflictResolution {
    /** Combine the metrics of all overlapping activities into one workout. */
    MERGE,

    /** Cut the new activity down to the part of its time range that no existing activity covers. */
    TRIM,

    /** Keep the existing activities and drop the new/edited one. */
    DISCARD
}

/**
 * Returned (as failure) by the save use cases when the activity overlaps [conflicting] ones and no
 * [ConflictResolution] was given. Nothing has been saved.
 *
 * @param canTrim false when the activity is completely covered by existing ones, so nothing would be left
 */
class ActivityConflictException(
    val conflicting: List<Activity>,
    val canTrim: Boolean
) : Exception("Activity overlaps ${conflicting.size} existing activities")
