package com.wandr.domain.usecase

import com.wandr.domain.repository.NotificationRepository

class GetNotificationsUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(userId: String) = repository.getNotifications(userId)
}
