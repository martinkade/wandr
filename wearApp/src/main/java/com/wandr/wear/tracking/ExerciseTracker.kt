package com.wandr.wear.tracking

import android.content.Context
import android.os.SystemClock
import androidx.concurrent.futures.await
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import com.wandr.domain.watch.WatchWorkout
import com.wandr.wear.model.LiveWorkoutState
import com.wandr.wear.model.WearActivityType
import com.wandr.wear.model.WorkoutPhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.util.UUID

/**
 * Records one workout with Health Services' [ExerciseClient]. Lives in the app graph so that the UI and
 * [WorkoutForegroundService] (which keeps the process alive with the screen off) share the same session.
 */
class ExerciseTracker(context: Context) {
    private val client: ExerciseClient = HealthServices.getClient(context.applicationContext).exerciseClient

    private val _state = MutableStateFlow(LiveWorkoutState())
    val state: StateFlow<LiveWorkoutState> = _state.asStateFlow()

    private var accumulator = WorkoutAccumulator()
    private var workoutId = ""
    private var startTimeMs = 0L
    private var activeDurationMs = 0L
    private var checkpointRealtimeMs = 0L
    private var running = false

    private val callback = object : ExerciseUpdateCallback {
        override fun onRegistered() = Unit
        override fun onRegistrationFailed(throwable: Throwable) = Unit
        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) = Unit
        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val bootInstant = Instant.ofEpochMilli(System.currentTimeMillis() - SystemClock.elapsedRealtime())
            val metrics = update.latestMetrics
            metrics.getData(DataType.DISTANCE_TOTAL)?.total?.let(accumulator::onDistanceTotal)
            metrics.getData(DataType.ELEVATION_GAIN_TOTAL)?.total?.let(accumulator::onElevationGainTotal)
            metrics.getData(DataType.HEART_RATE_BPM).forEach { accumulator.onHeartRate(it.value) }
            metrics.getData(DataType.LOCATION).forEach { sample ->
                val l = sample.value
                accumulator.onLocation(l.latitude, l.longitude, l.altitude, sample.getTimeInstant(bootInstant).toEpochMilli())
            }
            update.activeDurationCheckpoint?.let {
                activeDurationMs = it.activeDuration.toMillis()
                checkpointRealtimeMs = SystemClock.elapsedRealtime()
            }
            val s = update.exerciseStateInfo.state
            val phase = when {
                s == ExerciseState.ACTIVE -> WorkoutPhase.ACTIVE
                s.isPaused -> WorkoutPhase.PAUSED
                else -> _state.value.phase
            }
            publish(phase)
        }
    }

    /** Re-publishes the state with a fresh elapsed time; called once per second by the UI while a workout runs. */
    fun tick() = publish(_state.value.phase)

    suspend fun start(type: WearActivityType) {
        check(!running) { "A workout is already running" }
        accumulator = WorkoutAccumulator()
        workoutId = UUID.randomUUID().toString()
        startTimeMs = System.currentTimeMillis()
        activeDurationMs = 0L
        checkpointRealtimeMs = SystemClock.elapsedRealtime()
        _state.value = LiveWorkoutState(phase = WorkoutPhase.STARTING, type = type)

        try {
            val capabilities = client.getCapabilitiesAsync().await()
            val supported = capabilities.supportedExerciseTypes
            val exerciseType = when (type) {
                WearActivityType.HIKING -> if (ExerciseType.HIKING in supported) ExerciseType.HIKING else ExerciseType.WALKING
                WearActivityType.RUNNING -> ExerciseType.RUNNING
                WearActivityType.CYCLING -> ExerciseType.BIKING
            }
            val available = capabilities.getExerciseTypeCapabilities(exerciseType).supportedDataTypes
            val wanted = setOf(
                DataType.HEART_RATE_BPM,
                DataType.DISTANCE_TOTAL,
                DataType.ELEVATION_GAIN_TOTAL,
                DataType.LOCATION
            )
            val config = ExerciseConfig(
                exerciseType = exerciseType,
                dataTypes = wanted.filter { it in available }.toSet(),
                isAutoPauseAndResumeEnabled = false,
                isGpsEnabled = DataType.LOCATION in available
            )
            client.setUpdateCallback(callback)
            client.startExerciseAsync(config).await()
            running = true
            publish(WorkoutPhase.ACTIVE)
        } catch (e: Throwable) {
            runCatching { client.clearUpdateCallbackAsync(callback).await() }
            _state.value = LiveWorkoutState()
            throw e
        }
    }

    suspend fun pause() {
        client.pauseExerciseAsync().await()
        activeDurationMs = elapsedMs()
        checkpointRealtimeMs = SystemClock.elapsedRealtime()
        publish(WorkoutPhase.PAUSED)
    }

    suspend fun resume() {
        client.resumeExerciseAsync().await()
        checkpointRealtimeMs = SystemClock.elapsedRealtime()
        publish(WorkoutPhase.ACTIVE)
    }

    /** Ends the exercise and returns the recorded workout (a new random UUID id). */
    suspend fun stop(): WatchWorkout {
        val endTime = System.currentTimeMillis()
        try {
            client.endExerciseAsync().await()
        } finally {
            runCatching { client.clearUpdateCallbackAsync(callback).await() }
            running = false
        }
        val type = _state.value.type
        _state.value = LiveWorkoutState()
        return accumulator.build(workoutId, type.wireName, startTimeMs, endTime)
    }

    private fun elapsedMs(): Long =
        if (_state.value.phase == WorkoutPhase.ACTIVE) activeDurationMs + (SystemClock.elapsedRealtime() - checkpointRealtimeMs)
        else activeDurationMs

    private fun publish(phase: WorkoutPhase) {
        _state.value = _state.value.copy(
            phase = phase,
            distanceMeters = accumulator.distanceMeters,
            heartRate = accumulator.currentHeartRate,
            elevationGainMeters = accumulator.elevationGainMeters,
            elapsedMs = elapsedMs()
        )
    }
}
