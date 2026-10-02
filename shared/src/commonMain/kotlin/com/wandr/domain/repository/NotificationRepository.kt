package com.wandr.domain.repository

import com.wandr.domain.model.AppNotification

interface NotificationRepository {
    /** The newest notifications of [userId], newest first. */
    suspend fun getNotifications(userId: String, limit: Int = 50): Result<List<AppNotification>>
    suspend fun getUnreadCount(userId: String): Result<Int>
    suspend fun markRead(ids: List<String>): Result<Unit>
}
