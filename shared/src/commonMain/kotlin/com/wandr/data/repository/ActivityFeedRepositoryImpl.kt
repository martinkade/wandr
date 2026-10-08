package com.wandr.data.repository

import com.wandr.data.fit.FitFileStorage
import com.wandr.data.local.dao.ActivityDao
import com.wandr.data.local.entity.ActivityEntity
import com.wandr.data.local.dao.ProfileDao
import com.wandr.data.remote.ActivityDto
import com.wandr.data.remote.ActivityRouteDto
import com.wandr.data.remote.ProfileDto
import com.wandr.data.remote.supabaseResult
import com.wandr.data.remote.toEntity
import com.wandr.data.sync.CacheReconciler
import com.wandr.data.sync.CachedRow
import com.wandr.domain.repository.ActivityFeedRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlin.time.Clock
import kotlin.time.Instant

class ActivityFeedRepositoryImpl(
    private val supabase: SupabaseClient,
    private val activityDao: ActivityDao,
    private val profileDao: ProfileDao,
    private val fitFileStorage: FitFileStorage
) : ActivityFeedRepository {

    override suspend fun refreshTeam(teamId: String, userId: String): Result<Unit> =
        supabaseResult {
        val requestedAt = now()
        val remote = supabase.postgrest.from("activities").select {
            filter { eq("team_id", teamId) }
            order("start_time", Order.DESCENDING)
            limit(PAGE_SIZE)
        }.decodeList<ActivityDto>()
        store(remote, userId)
        removeDeleted(activityDao.getSyncedActivitiesForTeamOnce(teamId), remote, requestedAt)
    }

    override suspend fun refreshUser(userId: String): Result<Unit> = supabaseResult {
        val requestedAt = now()
        val remote = supabase.postgrest.from("activities").select {
            filter { eq("user_id", userId) }
            order("start_time", Order.DESCENDING)
            limit(PAGE_SIZE)
        }.decodeList<ActivityDto>()
        store(remote, userId)
        removeDeleted(activityDao.getSyncedActivitiesForUserOnce(userId), remote, requestedAt)
    }

    /**
     * Activities that were deleted on another device leave the cache (with their FIT file). The server returned a page of
     * the newest activities, so only cached activities inside that page's time window can be judged.
     */
    private suspend fun removeDeleted(cached: List<ActivityEntity>, remote: List<ActivityDto>, requestedAt: Long) {
        val windowStart = if (remote.size >= PAGE_SIZE) remote.minOf { Instant.parse(it.startTime).toEpochMilliseconds() } else null
        val stale = CacheReconciler.staleIds(
            // What changed here after the request left may simply not be in the answer yet.
            cached.map { CachedRow(it.id, isSynced = it.syncStatus == SYNCED && it.updatedAt < requestedAt, sortKey = it.startTime) },
            remote.map { it.id }.toSet(),
            windowStart
        ).toSet()
        cached.filter { it.id in stale }.forEach { activity ->
            activity.fitFilePath?.let(fitFileStorage::delete)
            activityDao.deleteActivity(activity.id)
        }
    }

    private suspend fun store(remote: List<ActivityDto>, currentUserId: String) {
        if (remote.isEmpty()) return

        // The server only returns routes the owner shares (or our own). If this call fails the cached routes stay.
        val routes: Map<String, String>? = supabaseResult {
            supabase.postgrest.from("activity_routes").select(Columns.list("activity_id", "user_id", "polyline")) {
                filter { isIn("activity_id", remote.map { it.id }) }
            }.decodeList<ActivityRouteDto>().associate { it.activityId to it.polyline }
        }.getOrNull()

        val entities = remote.mapNotNull { dto ->
            val local = activityDao.getActivityOnce(dto.id)
            // Unsynced local edits win; the sync engine pushes them. The local FIT file stays attached.
            if (local != null && local.syncStatus != SYNCED) return@mapNotNull null
            val own = dto.userId == currentUserId
            val polyline = when {
                routes == null -> local?.polyline
                // An own activity may have a route that was not uploaded yet (recorded before routes were synced).
                else -> routes[dto.id] ?: local?.polyline?.takeIf { own }
            }
            dto.toEntity(fitFilePath = local?.fitFilePath, polyline = polyline)
        }
        // One transaction for the whole page: the lists on the screens update once, not once per activity.
        activityDao.insertActivities(entities)

        // The authors of the other activities, for the name and avatar on the feed cards.
        val authorIds = remote.map { it.userId }.distinct().filter { it != currentUserId }
        if (authorIds.isNotEmpty()) {
            supabaseResult {
                supabase.postgrest.from("profiles").select { filter { isIn("id", authorIds) } }
                    .decodeList<ProfileDto>()
                    .forEach { dto ->
                        val local = profileDao.getProfileOnce(dto.id)
                        if (local == null || local.syncStatus == SYNCED) profileDao.insertProfile(dto.toEntity())
                    }
            }
        }
    }

    private fun now() = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val PAGE_SIZE = 100L
        const val SYNCED = "SYNCED"
    }
}
