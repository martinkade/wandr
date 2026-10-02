package com.wandr.android.health

import android.content.Context
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.wandr.domain.watch.ExternalWorkoutId
import com.wandr.domain.watch.WatchWorkout
import com.wandr.domain.watch.WorkoutSource
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Reads workouts from Health Connect. Strictly read-only: only the three read permissions below are requested, and
 * nothing is ever written back. Workouts written by this app itself are skipped.
 */
class HealthConnectImporter(private val context: Context) {

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class)
    )

    private val prefs = context.getSharedPreferences("health_connect_import", Context.MODE_PRIVATE)

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun hasPermissions(): Boolean = client.permissionController.getGrantedPermissions().containsAll(permissions)

    /**
     * Workouts since the last successful read (the first read looks back 30 days). Reading the same workout twice is
     * harmless: its id is derived from the Health Connect record id and the import skips known ids.
     */
    suspend fun readWorkouts(now: Instant = Instant.now()): List<WatchWorkout> {
        val since = prefs.getLong(KEY_LAST_READ, 0L).takeIf { it > 0 }
            ?.let { Instant.ofEpochMilli(it).minus(1, ChronoUnit.DAYS) } // late-synced records
            ?: now.minus(INITIAL_LOOKBACK_DAYS, ChronoUnit.DAYS)

        val workouts = mutableListOf<WatchWorkout>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(since, now), pageToken = pageToken)
            )
            response.records.forEach { session -> toWorkout(session)?.let(workouts::add) }
            pageToken = response.pageToken
        } while (pageToken != null)

        prefs.edit { putLong(KEY_LAST_READ, now.toEpochMilli()) }
        return workouts
    }

    private suspend fun toWorkout(session: ExerciseSessionRecord): WatchWorkout? {
        val type = HealthConnectTypes.activityType(session.exerciseType) ?: return null
        val origin = session.metadata.dataOrigin
        if (origin.packageName == context.packageName) return null
        if (!session.endTime.isAfter(session.startTime)) return null

        val totals = client.aggregate(
            AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL, ElevationGainedRecord.ELEVATION_GAINED_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime),
                dataOriginFilter = setOf(origin)
            )
        )
        return WatchWorkout(
            id = ExternalWorkoutId.from(WorkoutSource.HEALTH_CONNECT, session.metadata.id),
            activityType = type,
            startTime = session.startTime.toEpochMilli(),
            endTime = session.endTime.toEpochMilli(),
            distanceMeters = totals[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0,
            elevationGainMeters = totals[ElevationGainedRecord.ELEVATION_GAINED_TOTAL]?.inMeters ?: 0.0,
            source = WorkoutSource.HEALTH_CONNECT
        )
    }

    private companion object {
        const val KEY_LAST_READ = "last_read_ms"
        const val INITIAL_LOOKBACK_DAYS = 30L
    }
}
