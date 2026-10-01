package com.wandr.domain.repository

import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun getUserActivities(userId: String): Flow<List<Activity>>
    fun getTeamActivities(teamId: String): Flow<List<Activity>>
    suspend fun getOverlappingActivities(userId: String, startTime: Long, endTime: Long): List<Activity>
    suspend fun saveActivity(activity: Activity, trackpoints: List<GpsTrackpoint>?): Result<Activity>
    suspend fun deleteActivity(id: String): Result<Unit>
}
