package com.wandr.domain.settings

/** Small key-value settings of this device (SharedPreferences on Android, NSUserDefaults on iOS). */
interface PreferenceStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}
