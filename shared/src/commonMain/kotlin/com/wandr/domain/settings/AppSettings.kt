package com.wandr.domain.settings

/** The settings of the app on this device. */
class AppSettings(private val store: PreferenceStore) {
    /** Push notifications for likes, comments and reactions on this device; on by default. */
    var pushNotificationsEnabled: Boolean
        get() = store.getBoolean(KEY_PUSH_ENABLED, true)
        set(value) = store.putBoolean(KEY_PUSH_ENABLED, value)

    private companion object {
        const val KEY_PUSH_ENABLED = "push_notifications_enabled"
    }
}
