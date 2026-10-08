package com.wandr.data.repository

import com.wandr.data.fit.FitFileDecoder
import com.wandr.data.fit.FitFileEncoder
import com.wandr.data.fit.FitFileStorage
import com.wandr.data.local.dao.ActivityDao
import com.wandr.data.local.entity.ActivityEntity
import com.wandr.data.localResult
import com.wandr.domain.geo.PolylineCodec
import com.wandr.domain.geo.TrackSimplifier
import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.repository.ActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map

class ActivityRepositoryImpl(
    private val activityDao: ActivityDao,
    private val fitFileStorage: FitFileStorage,
    /** Called after every local change that has to reach the server (create, edit, delete), e.g. to start the sync. */
    private val onLocalChange: () -> Unit = {}
) : ActivityRepository {

    override fun getUserActivities(userId: String): Flow<List<Activity>> {
        return activityDao.getActivitiesForUser(userId).map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.Default) // the screens collect on the main thread; mapping the list is not their job
    }

    override fun getActivityById(id: String): Flow<Activity?> =
        activityDao.getActivityById(id).map { it?.toDomain() }.flowOn(Dispatchers.Default)

    override suspend fun getTrackpoints(activityId: String): List<GpsTrackpoint> {
        val path = activityDao.getActivityOnce(activityId)?.fitFilePath ?: return emptyList()
        // Reading and decoding a FIT file is work for a background thread, whoever asks.
        return withContext(Dispatchers.Default) {
            val bytes = fitFileStorage.read(path) ?: return@withContext emptyList()
            FitFileDecoder.decodeTrackpoints(bytes)
        }
    }

    override fun getUserActivityCount(userId: String): Flow<Int> = activityDao.getActivityCountForUser(userId)

    override fun getTeamActivities(teamId: String): Flow<List<Activity>> {
        return activityDao.getActivitiesForTeam(teamId).map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun getOverlappingActivities(userId: String, startTime: Long, endTime: Long): List<Activity> {
        return activityDao.getOverlappingActivities(userId, startTime, endTime).map { it.toDomain() }
    }

    override suspend fun saveActivity(activity: Activity, trackpoints: List<GpsTrackpoint>?): Result<Activity> {
        return localResult { withContext(Dispatchers.Default) {
            var fitFilePath = activity.fitFilePath
            var polyline = activity.polyline

            if (!trackpoints.isNullOrEmpty()) {
                val fitBytes = FitFileEncoder.encode(
                    activityType = activity.activityType,
                    startTimeMs = activity.startTime,
                    endTimeMs = activity.endTime,
                    distanceMeters = activity.distanceMeters,
                    elevationGainMeters = activity.elevationGainMeters,
                    trackpoints = trackpoints
                )
                
                // The FIT file stays on this device; it is never uploaded. A write failure fails the save, so a
                // recorded track is not silently dropped.
                fitFilePath = fitFileStorage.save(activity.id, fitBytes)
                // Only the simplified route goes to the server (see activity_routes).
                polyline = PolylineCodec.encode(TrackSimplifier.simplify(trackpoints))
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
                showMap = activity.showMap,
                polyline = polyline,
                syncStatus = "DIRTY"
            )

            activityDao.insertActivity(entity)
            onLocalChange()
            entity.toDomain()
        } }
    }

    override suspend fun deleteActivity(id: String): Result<Unit> {
        return localResult {
            activityDao.getActivityOnce(id)?.fitFilePath?.let(fitFileStorage::delete)
            activityDao.markDeleted(id) // the sync engine deletes it on the server and then removes the row
            onLocalChange()
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
            updatedAt = updatedAt,
            showMap = showMap,
            polyline = polyline
        )
    }
}
