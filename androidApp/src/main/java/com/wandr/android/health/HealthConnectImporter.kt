package com.wandr.android.health

import android.content.Context
import androidx.core.content.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.wandr.domain.watch.ExternalWorkoutId
import com.wandr.domain.watch.WatchWorkout
import com.wandr.domain.watch.WorkoutSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Today's steps and floors climbed; a value is null when its permission was not granted. */
data class DailyHealthSummary(val steps: Long?, val floors: Double?)

/**
 * Reads workouts, steps and floors from Health Connect. Strictly read-only: only the three read permissions below are requested, and
 * nothing is ever written back. Workouts written by this app itself are skipped.
 */
class HealthConnectImporter(private val context: Context) {

    /** Needed to import workouts. */
    val workoutPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class)
    )

    private val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val floorsPermission = HealthPermission.getReadPermission(FloorsClimbedRecord::class)

    /** Everything that is requested together; the user may grant only some of them. */
    val permissions: Set<String> = workoutPermissions + stepsPermission + floorsPermission

    private val prefs = context.getSharedPreferences("health_connect_import", Context.MODE_PRIVATE)

    /** When workouts were last read (epoch ms), 0 if never. */
    val lastReadAt: Long get() = prefs.getLong(KEY_LAST_READ, 0L)

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    /** True if workouts can be read (steps and floors are optional). */
    suspend fun hasPermissions(): Boolean = client.permissionController.getGrantedPermissions().containsAll(workoutPermissions)

    /**
     * Whether the permission request should be shown: workouts cannot be read yet, or steps/floors were never asked
     * for (e.g. by an earlier version). A user who declined steps/floors is not asked again on every tap.
     */
    suspend fun shouldRequestPermissions(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return !granted.containsAll(workoutPermissions) || (!granted.containsAll(permissions) && !prefs.getBoolean(KEY_ASKED_ALL, false))
    }

    fun markRequested() = prefs.edit { putBoolean(KEY_ASKED_ALL, true) }

    /**
     * Steps and floors climbed since midnight (local time), or null if neither may be read. Aggregated by Health
     * Connect, which already de-duplicates data written by several apps.
     */
    suspend fun readToday(now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): DailyHealthSummary? {
        val granted = client.permissionController.getGrantedPermissions()
        val canReadSteps = stepsPermission in granted
        val canReadFloors = floorsPermission in granted
        if (!canReadSteps && !canReadFloors) return null

        val midnight = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val metrics = buildSet {
            if (canReadSteps) add(StepsRecord.COUNT_TOTAL)
            if (canReadFloors) add(FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL)
        }
        val totals = client.aggregate(AggregateRequest(metrics, TimeRangeFilter.between(midnight, now)))
        return DailyHealthSummary(
            steps = if (canReadSteps) totals[StepsRecord.COUNT_TOTAL] ?: 0L else null,
            floors = if (canReadFloors) totals[FloorsClimbedRecord.FLOORS_CLIMBED_TOTAL] ?: 0.0 else null
        )
    }

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
        const val KEY_ASKED_ALL = "asked_all_permissions"
        const val INITIAL_LOOKBACK_DAYS = 30L
    }
}
