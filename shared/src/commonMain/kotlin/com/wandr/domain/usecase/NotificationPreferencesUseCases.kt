package com.wandr.domain.usecase

import com.wandr.domain.model.NotificationPreferences
import com.wandr.domain.repository.NotificationPreferencesRepository

class GetNotificationPreferencesUseCase(private val repository: NotificationPreferencesRepository) {
    suspend operator fun invoke(userId: String) = repository.get(userId)
}

class SaveNotificationPreferencesUseCase(private val repository: NotificationPreferencesRepository) {
    suspend operator fun invoke(userId: String, preferences: NotificationPreferences) =
        repository.save(userId, preferences)
}
