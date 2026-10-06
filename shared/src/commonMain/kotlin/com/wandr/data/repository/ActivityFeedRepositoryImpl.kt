package com.wandr.data.repository

import com.wandr.data.local.dao.ActivityDao
import com.wandr.data.local.dao.ProfileDao
import com.wandr.data.remote.ActivityDto
import com.wandr.data.remote.ActivityRouteDto
import com.wandr.data.remote.ProfileDto
import com.wandr.data.remote.toEntity
import com.wandr.domain.repository.ActivityFeedRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

class ActivityFeedRepositoryImpl(
    private val supabase: SupabaseClient,
    private val activityDao: ActivityDao,
    private val profileDao: ProfileDao
) : ActivityFeedRepository {

    override suspend fun refreshTeam(teamId: String, userId: String): Result<Unit> = runCatching {
        store(supabase.postgrest.from("activities").select {
            filter { eq("team_id", teamId) }
            order("start_time", Order.DESCENDING)
            limit(PAGE_SIZE)
        }.decodeList<ActivityDto>(), userId)
    }

    override suspend fun refreshUser(userId: String): Result<Unit> = runCatching {
        store(supabase.postgrest.from("activities").select {
            filter { eq("user_id", userId) }
            order("start_time", Order.DESCENDING)
            limit(PAGE_SIZE)
        }.decodeList<ActivityDto>(), userId)
    }

    private suspend fun store(remote: List<ActivityDto>, currentUserId: String) {
        if (remote.isEmpty()) return

        // The server only returns routes the owner shares (or our own). If this call fails the cached routes stay.
        val routes: Map<String, String>? = runCatching {
            supabase.postgrest.from("activity_routes").select(Columns.list("activity_id", "user_id", "polyline")) {
                filter { isIn("activity_id", remote.map { it.id }) }
            }.decodeList<ActivityRouteDto>().associate { it.activityId to it.polyline }
        }.getOrNull()

        remote.forEach { dto ->
            val local = activityDao.getActivityOnce(dto.id)
            // Unsynced local edits win; the sync engine pushes them. The local FIT file stays attached.
            if (local != null && local.syncStatus != SYNCED) return@forEach
            val own = dto.userId == currentUserId
            val polyline = when {
                routes == null -> local?.polyline
                // An own activity may have a route that was not uploaded yet (recorded before routes were synced).
                else -> routes[dto.id] ?: local?.polyline?.takeIf { own }
            }
            activityDao.insertActivity(dto.toEntity(fitFilePath = local?.fitFilePath, polyline = polyline))
        }

        // The authors of the other activities, for the name and avatar on the feed cards.
        val authorIds = remote.map { it.userId }.distinct().filter { it != currentUserId }
        if (authorIds.isNotEmpty()) {
            runCatching {
                supabase.postgrest.from("profiles").select { filter { isIn("id", authorIds) } }
                    .decodeList<ProfileDto>()
                    .forEach { dto ->
                        val local = profileDao.getProfileOnce(dto.id)
                        if (local == null || local.syncStatus == SYNCED) profileDao.insertProfile(dto.toEntity())
                    }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 100L
        const val SYNCED = "SYNCED"
    }
}
