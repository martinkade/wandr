package com.wandr.data.repository

import com.wandr.data.local.dao.ActivityDao
import com.wandr.data.remote.ActivityDto
import com.wandr.data.remote.toEntity
import com.wandr.domain.repository.ActivityFeedRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class ActivityFeedRepositoryImpl(
    private val supabase: SupabaseClient,
    private val activityDao: ActivityDao
) : ActivityFeedRepository {

    override suspend fun refreshTeam(teamId: String): Result<Unit> = runCatching {
        store(supabase.postgrest.from("activities").select {
            filter { eq("team_id", teamId) }
            order("start_time", Order.DESCENDING)
            limit(PAGE_SIZE)
        }.decodeList<ActivityDto>())
    }

    override suspend fun refreshUser(userId: String): Result<Unit> = runCatching {
        store(supabase.postgrest.from("activities").select {
            filter { eq("user_id", userId) }
            order("start_time", Order.DESCENDING)
            limit(PAGE_SIZE)
        }.decodeList<ActivityDto>())
    }

    private suspend fun store(remote: List<ActivityDto>) {
        remote.forEach { dto ->
            val local = activityDao.getActivityOnce(dto.id)
            // Unsynced local edits win; the sync engine pushes them. The local FIT file stays attached.
            if (local != null && local.syncStatus != "SYNCED") return@forEach
            activityDao.insertActivity(dto.toEntity(fitFilePath = local?.fitFilePath))
        }
    }

    private companion object {
        const val PAGE_SIZE = 100L
    }
}
