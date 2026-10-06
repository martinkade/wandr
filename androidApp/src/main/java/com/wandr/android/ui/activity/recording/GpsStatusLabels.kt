package com.wandr.android.ui.activity.recording

import androidx.annotation.StringRes
import com.wandr.android.R
import com.wandr.presentation.activity.GpsStatus

@StringRes
internal fun GpsStatus.labelRes(): Int = when (this) {
    GpsStatus.SEARCHING -> R.string.recording_gps_searching
    GpsStatus.WEAK -> R.string.recording_gps_weak
    GpsStatus.GOOD -> R.string.recording_gps_good
}
