package com.wandr.data.settings

import com.wandr.domain.settings.PreferenceStore
import platform.Foundation.NSUserDefaults

class UserDefaultsStore : PreferenceStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = key)
    }
}
