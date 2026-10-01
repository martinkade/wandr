package com.wandr.presentation.activity

import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.usecase.CreateManualActivityUseCase
import com.wandr.domain.usecase.DeleteActivityUseCase
import com.wandr.domain.usecase.GetTeamActivitiesUseCase
import com.wandr.domain.usecase.GetUserActivitiesUseCase
import com.wandr.domain.usecase.RecordGpsActivityUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class ActivityViewModel(
    private val getUserActivitiesUseCase: GetUserActivitiesUseCase,
    private val getTeamActivitiesUseCase: GetTeamActivitiesUseCase,
    private val createManualActivityUseCase: CreateManualActivityUseCase,
    private val recordGpsActivityUseCase: RecordGpsActivityUseCase,
    private val deleteActivityUseCase: DeleteActivityUseCase
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(ActivityState())
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    private var trackingStartTime: Long = 0

    fun processIntent(intent: ActivityIntent) {
        when (intent) {
            is ActivityIntent.LoadUserActivities -> loadUserActivities(intent.userId)
            is ActivityIntent.LoadTeamActivities -> loadTeamActivities(intent.teamId)
            is ActivityIntent.UpdateManualTitle -> _state.update { it.copy(manualTitle = intent.title) }
            is ActivityIntent.UpdateManualDescription -> _state.update { it.copy(manualDescription = intent.description) }
            is ActivityIntent.UpdateManualActivityType -> _state.update { it.copy(manualActivityType = intent.type) }
            is ActivityIntent.UpdateManualDistance -> _state.update { it.copy(manualDistanceKm = intent.km) }
            is ActivityIntent.UpdateManualDuration -> _state.update { it.copy(manualDurationMinutes = intent.minutes) }
            is ActivityIntent.UpdateManualElevation -> _state.update { it.copy(manualElevationMeters = intent.meters) }
            is ActivityIntent.SubmitManualActivity -> submitManualActivity(intent.userId, intent.teamId)
            is ActivityIntent.StartGpsTracking -> startGpsTracking(intent.activityType)
            is ActivityIntent.AddTrackpoint -> addTrackpoint(intent.trackpoint)
            is ActivityIntent.PauseGpsTracking -> _state.update { it.copy(isPaused = true) }
            is ActivityIntent.ResumeGpsTracking -> _state.update { it.copy(isPaused = false) }
            is ActivityIntent.StopAndSaveGpsTracking -> stopAndSaveGpsTracking(intent.userId, intent.teamId, intent.title)
            is ActivityIntent.DeleteActivity -> deleteActivity(intent.activityId)
            is ActivityIntent.ClearMessages -> _state.update { it.copy(errorMessage = null, successMessage = null) }
        }
    }

    private fun loadUserActivities(userId: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            getUserActivitiesUseCase(userId).collect { list ->
                _state.update { it.copy(activities = list, isLoading = false) }
            }
        }
    }

    private fun loadTeamActivities(teamId: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            getTeamActivitiesUseCase(teamId).collect { list ->
                _state.update { it.copy(activities = list, isLoading = false) }
            }
        }
    }

    private fun submitManualActivity(userId: String, teamId: String?) {
        scope.launch {
            val currentState = _state.value
            if (currentState.manualTitle.isBlank()) {
                _state.update { it.copy(errorMessage = "Title cannot be empty") }
                return@launch
            }
            _state.update { it.copy(isLoading = true) }
            val startTime = Clock.System.now().toEpochMilliseconds() - (currentState.manualDurationMinutes * 60 * 1000).toLong()
            val result = createManualActivityUseCase(
                userId = userId,
                teamId = teamId,
                title = currentState.manualTitle,
                description = currentState.manualDescription.ifBlank { null },
                activityType = currentState.manualActivityType,
                distanceMeters = currentState.manualDistanceKm * 1000.0,
                durationSeconds = currentState.manualDurationMinutes * 60.0,
                elevationGainMeters = currentState.manualElevationMeters,
                startTime = startTime
            )
            result.fold(
                onSuccess = {
                    _state.update { s ->
                        s.copy(
                            isLoading = false,
                            successMessage = "Activity created successfully!",
                            manualTitle = "",
                            manualDescription = ""
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { s -> s.copy(isLoading = false, errorMessage = error.message ?: "Failed to create activity") }
                }
            )
        }
    }

    private fun startGpsTracking(activityType: String) {
        trackingStartTime = Clock.System.now().toEpochMilliseconds()
        _state.update {
            it.copy(
                isTracking = true,
                isPaused = false,
                liveDistanceMeters = 0.0,
                liveDurationSeconds = 0.0,
                liveElevationGainMeters = 0.0,
                liveTrackpoints = emptyList(),
                manualActivityType = activityType
            )
        }
    }

    private fun addTrackpoint(trackpoint: GpsTrackpoint) {
        if (!_state.value.isTracking || _state.value.isPaused) return
        _state.update { s ->
            val updatedPoints = s.liveTrackpoints + trackpoint
            var addedDistance = 0.0
            if (s.liveTrackpoints.isNotEmpty()) {
                val last = s.liveTrackpoints.last()
                addedDistance = calculateDistanceMeters(last.latitude, last.longitude, trackpoint.latitude, trackpoint.longitude)
            }
            val elapsedSec = (trackpoint.timestamp - trackingStartTime) / 1000.0
            s.copy(
                liveTrackpoints = updatedPoints,
                liveDistanceMeters = s.liveDistanceMeters + addedDistance,
                liveDurationSeconds = elapsedSec.coerceAtLeast(0.0)
            )
        }
    }

    private fun stopAndSaveGpsTracking(userId: String, teamId: String?, title: String) {
        scope.launch {
            val currentState = _state.value
            _state.update { it.copy(isLoading = true, isTracking = false) }
            val endTime = Clock.System.now().toEpochMilliseconds()
            val result = recordGpsActivityUseCase(
                userId = userId,
                teamId = teamId,
                title = title.ifBlank { "${currentState.manualActivityType.replaceFirstChar { c -> c.uppercase() }} Workout" },
                description = "Recorded with WANDR GPS Tracker",
                activityType = currentState.manualActivityType,
                distanceMeters = currentState.liveDistanceMeters,
                durationSeconds = currentState.liveDurationSeconds,
                elevationGainMeters = currentState.liveElevationGainMeters,
                startTime = trackingStartTime,
                endTime = endTime,
                trackpoints = currentState.liveTrackpoints
            )
            result.fold(
                onSuccess = {
                    _state.update { s ->
                        s.copy(
                            isLoading = false,
                            successMessage = "GPS Activity recorded and saved!",
                            liveTrackpoints = emptyList(),
                            liveDistanceMeters = 0.0,
                            liveDurationSeconds = 0.0
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { s -> s.copy(isLoading = false, errorMessage = error.message ?: "Failed to save GPS activity") }
                }
            )
        }
    }

    private fun deleteActivity(activityId: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            deleteActivityUseCase(activityId).fold(
                onSuccess = {
                    _state.update { s -> s.copy(isLoading = false, successMessage = "Activity deleted") }
                },
                onFailure = { error ->
                    _state.update { s -> s.copy(isLoading = false, errorMessage = error.message ?: "Failed to delete activity") }
                }
            )
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = (lat2 - lat1) * (kotlin.math.PI / 180.0)
        val dLon = (lon2 - lon1) * (kotlin.math.PI / 180.0)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(lat1 * (kotlin.math.PI / 180.0)) * kotlin.math.cos(lat2 * (kotlin.math.PI / 180.0)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }
}
