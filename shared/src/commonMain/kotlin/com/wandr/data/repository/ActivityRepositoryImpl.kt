package com.wandr.data.repository

import com.wandr.data.cache.LruFileCache
import com.wandr.data.fit.FitFileEncoder
import com.wandr.data.local.dao.ActivityDao
import com.wandr.data.local.entity.ActivityEntity
import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.repository.ActivityRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ActivityRepositoryImpl(
    private val activityDao: ActivityDao,
    private val lruFileCache: LruFileCache,
    private val supabase: SupabaseClient
) : ActivityRepository {

    override fun getUserActivities(userId: String): Flow<List<Activity>> {
        return activityDao.getActivitiesForUser(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getUserActivityCount(userId: String): Flow<Int> = activityDao.getActivityCountForUser(userId)

    override fun getTeamActivities(teamId: String): Flow<List<Activity>> {
        return activityDao.getActivitiesForTeam(teamId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getOverlappingActivities(userId: String, startTime: Long, endTime: Long): List<Activity> {
        return activityDao.getOverlappingActivities(userId, startTime, endTime).map { it.toDomain() }
    }

    override suspend fun saveActivity(activity: Activity, trackpoints: List<GpsTrackpoint>?): Result<Activity> {
        return runCatching {
            var fitFilePath = activity.fitFilePath

            if (!trackpoints.isNullOrEmpty()) {
                val fitBytes = FitFileEncoder.encode(
                    activityType = activity.activityType,
                    startTimeMs = activity.startTime,
                    endTimeMs = activity.endTime,
                    distanceMeters = activity.distanceMeters,
                    elevationGainMeters = activity.elevationGainMeters,
                    trackpoints = trackpoints
                )
                
                val cacheKey = "fit_${activity.id}.fit"
                lruFileCache.put(cacheKey, fitBytes)
                
                try {
                    val bucket = supabase.storage.from("fit-files")
                    bucket.upload(cacheKey, fitBytes) { upsert = true }
                    fitFilePath = bucket.publicUrl(cacheKey)
                } catch (e: Exception) {
                    fitFilePath = cacheKey
                }
            }

            val entity = ActivityEntity(
                id = activity.id,
                userId = activity.userId,
                teamId = activity.teamId,
                title = activity.title,
                description = activity.description,
                activityType = activity.activityType,
                distanceMeters = activity.distanceMeters,
                durationSeconds = activity.durationSeconds,
                elevationGainMeters = activity.elevationGainMeters,
                fitFilePath = fitFilePath,
                startTime = activity.startTime,
                endTime = activity.endTime,
                isManualEntry = activity.isManualEntry,
                createdAt = activity.createdAt,
                updatedAt = activity.updatedAt,
                syncStatus = "DIRTY"
            )

            activityDao.insertActivity(entity)
            entity.toDomain()
        }
    }

    override suspend fun deleteActivity(id: String): Result<Unit> {
        return runCatching {
            activityDao.deleteActivity(id)
        }
    }

    private fun ActivityEntity.toDomain(): Activity {
        return Activity(
            id = id,
            userId = userId,
            teamId = teamId,
            title = title,
            description = description,
            activityType = activityType,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            elevationGainMeters = elevationGainMeters,
            fitFilePath = fitFilePath,
            startTime = startTime,
            endTime = endTime,
            isManualEntry = isManualEntry,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
