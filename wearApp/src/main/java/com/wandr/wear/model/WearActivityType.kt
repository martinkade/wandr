package com.wandr.wear.model

import androidx.annotation.StringRes
import com.wandr.wear.R

/** The activity types the watch can record. [wireName] is the `activity_type` of the WatchWorkout JSON. */
enum class WearActivityType(val wireName: String, @StringRes val labelRes: Int) {
    HIKING("hiking", R.string.type_hiking),
    RUNNING("running", R.string.type_running),
    CYCLING("cycling", R.string.type_cycling)
}
