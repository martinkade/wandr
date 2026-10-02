package com.wandr.domain.repository

import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun getUserActivities(userId: String): Flow<List<Activity>>
    fun getActivityById(id: String): Flow<Activity?>

    /**
     * The recorded GPS track of [activityId], read from the `.FIT` file on THIS device. Empty for manual entries and
     * for activities recorded on another device (the file is never uploaded).
     */
    suspend fun getTrackpoints(activityId: String): List<GpsTrackpoint>
    fun getUserActivityCount(userId: String): Flow<Int>
    fun getTeamActivities(teamId: String): Flow<List<Activity>>
    suspend fun getOverlappingActivities(userId: String, startTime: Long, endTime: Long): List<Activity>
    suspend fun saveActivity(activity: Activity, trackpoints: List<GpsTrackpoint>?): Result<Activity>
    suspend fun deleteActivity(id: String): Result<Unit>
}
