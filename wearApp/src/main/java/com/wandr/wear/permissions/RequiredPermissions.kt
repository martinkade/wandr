package com.wandr.wear.permissions

/** The runtime permissions a workout needs, depending on the Android version. Pure, so it is unit-testable. */
object RequiredPermissions {
    const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    const val ACTIVITY_RECOGNITION = "android.permission.ACTIVITY_RECOGNITION"
    const val BODY_SENSORS = "android.permission.BODY_SENSORS"
    const val READ_HEART_RATE = "android.permission.health.READ_HEART_RATE"
    const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

    fun forSdk(sdkInt: Int): List<String> = buildList {
        add(FINE_LOCATION)
        add(ACTIVITY_RECOGNITION)
        add(if (sdkInt >= 36) READ_HEART_RATE else BODY_SENSORS)
        if (sdkInt >= 33) add(POST_NOTIFICATIONS)
    }
}
