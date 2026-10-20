package com.wandr.domain.repository

import com.wandr.domain.model.NotificationPreferences

interface NotificationPreferencesRepository {
    /** The user's preferences; the defaults (all on) when they never changed them. */
    suspend fun get(userId: String): Result<NotificationPreferences>
    suspend fun save(userId: String, preferences: NotificationPreferences): Result<Unit>
}
