package com.wandr.data.repository

import com.wandr.data.remote.NOTIFICATION_COLUMNS
import com.wandr.data.remote.NotificationDto
import com.wandr.domain.model.AppNotification
import com.wandr.domain.repository.NotificationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class NotificationRepositoryImpl(private val supabase: SupabaseClient) : NotificationRepository {

    override suspend fun getNotifications(userId: String, limit: Int): Result<List<AppNotification>> = runCatching {
        supabase.postgrest.from("notifications").select(Columns.raw(NOTIFICATION_COLUMNS)) {
            filter { eq("recipient_user_id", userId) }
            order("created_at", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList<NotificationDto>().map { it.toDomain() }
    }

    override suspend fun getUnreadCount(userId: String): Result<Int> = runCatching {
        val result = supabase.postgrest.from("notifications").select(Columns.list("id")) {
            count(Count.EXACT)
            filter {
                eq("recipient_user_id", userId)
                eq("is_read", false)
            }
            limit(1)
        }
        (result.countOrNull() ?: 0L).toInt()
    }

    override suspend fun markRead(ids: List<String>): Result<Unit> = runCatching {
        if (ids.isEmpty()) return@runCatching
        supabase.postgrest.from("notifications").update(buildJsonObject { put("is_read", true) }) {
            filter { isIn("id", ids) }
        }
    }
}
