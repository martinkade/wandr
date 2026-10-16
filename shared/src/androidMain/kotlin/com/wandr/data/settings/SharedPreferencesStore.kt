package com.wandr.data.settings

import android.content.Context
import com.wandr.domain.settings.PreferenceStore

class SharedPreferencesStore(context: Context) : PreferenceStore {
    private val preferences = context.applicationContext.getSharedPreferences("wandr_settings", Context.MODE_PRIVATE)

    override fun getBoolean(key: String, default: Boolean): Boolean = preferences.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }
}
