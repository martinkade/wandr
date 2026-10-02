package com.wandr.domain.usecase

import com.wandr.domain.repository.NotificationRepository

class MarkNotificationsReadUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(ids: List<String>) = repository.markRead(ids)
}
