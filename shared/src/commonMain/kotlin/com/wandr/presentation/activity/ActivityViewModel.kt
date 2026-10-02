package com.wandr.presentation.activity

import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.usecase.CreateManualActivityUseCase
import com.wandr.domain.usecase.DeleteActivityUseCase
import com.wandr.domain.usecase.GetActivityTrackUseCase
import com.wandr.domain.usecase.GetActivityUseCase
import com.wandr.domain.usecase.GetTeamActivitiesUseCase
import com.wandr.domain.usecase.GetUserActivitiesUseCase
import com.wandr.domain.usecase.RecordGpsActivityUseCase
import com.wandr.domain.usecase.UpdateActivityUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val getActivityUseCase: GetActivityUseCase,
    private val getActivityTrackUseCase: GetActivityTrackUseCase,
    private val createManualActivityUseCase: CreateManualActivityUseCase,
    private val updateActivityUseCase: UpdateActivityUseCase,
    private val recordGpsActivityUseCase: RecordGpsActivityUseCase,
    private val deleteActivityUseCase: DeleteActivityUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(ActivityState())
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    private var trackingStartTime: Long = 0
    private var listJob: Job? = null
    private var selectJob: Job? = null
    private var currentUserId: String? = null

    fun processIntent(intent: ActivityIntent) {
        when (intent) {
            is ActivityIntent.LoadUserActivities -> loadUserActivities(intent.userId)
            is ActivityIntent.LoadTeamActivities -> loadTeamActivities(intent.teamId)
            is ActivityIntent.SelectActivity -> selectActivity(intent.activityId)
            is ActivityIntent.StartCreate -> startCreate()
            is ActivityIntent.StartEdit -> startEdit(intent.activityId)
            is ActivityIntent.TitleChanged -> updateForm { it.copy(title = intent.title) }
            is ActivityIntent.DescriptionChanged -> updateForm { it.copy(description = intent.description) }
            is ActivityIntent.TypeChanged -> updateForm { it.copy(activityType = intent.type) }
            is ActivityIntent.DistanceChanged -> updateForm { it.copy(distanceKm = intent.km) }
            is ActivityIntent.DurationChanged -> updateForm { it.copy(durationMinutes = intent.minutes) }
            is ActivityIntent.ElevationChanged -> updateForm { it.copy(elevationMeters = intent.meters) }
            is ActivityIntent.StartTimeChanged -> updateForm { it.copy(startTime = intent.millis) }
            is ActivityIntent.SubmitForm -> submit(intent.userId, intent.teamId)
            is ActivityIntent.DiscardForm -> _state.update { it.copy(form = null) }
            is ActivityIntent.StartGpsTracking -> startGpsTracking(intent.activityType)
            is ActivityIntent.AddTrackpoint -> addTrackpoint(intent.trackpoint)
            is ActivityIntent.PauseGpsTracking -> _state.update { it.copy(isPaused = true) }
            is ActivityIntent.ResumeGpsTracking -> _state.update { it.copy(isPaused = false) }
            is ActivityIntent.StopAndSaveGpsTracking -> stopAndSaveGpsTracking(intent.userId, intent.teamId, intent.title)
            is ActivityIntent.DeleteActivity -> deleteActivity(intent.activityId)
            is ActivityIntent.ClearMessages -> _state.update { it.copy(errorMessage = null, success = null) }
        }
    }

    private fun loadUserActivities(userId: String) {
        currentUserId = userId
        _state.update { it.copy(isLoading = it.activities.isEmpty(), canEdit = it.selectedActivity?.userId == userId) }
        listJob?.cancel()
        listJob = scope.launch {
            getUserActivitiesUseCase(userId).collect { list ->
                _state.update { it.copy(activities = list, isLoading = false) }
            }
        }
    }

    private fun loadTeamActivities(teamId: String) {
        listJob?.cancel()
        listJob = scope.launch {
            _state.update { it.copy(isLoading = true) }
            getTeamActivitiesUseCase(teamId).collect { list ->
                _state.update { it.copy(activities = list, isLoading = false) }
            }
        }
    }

    private fun selectActivity(activityId: String) {
        _state.update { it.copy(selectedTrack = emptyList(), canEdit = false) }
        selectJob?.cancel()
        selectJob = scope.launch {
            var trackLoadedFor: String? = null
            getActivityUseCase(activityId).collect { activity ->
                _state.update { it.copy(selectedActivity = activity, canEdit = activity?.userId == currentUserId) }
                // The track is read from the .FIT file on this device, once per file.
                if (activity?.fitFilePath != null && trackLoadedFor != activity.fitFilePath) {
                    trackLoadedFor = activity.fitFilePath
                    val track = getActivityTrackUseCase(activity.id)
                    _state.update { it.copy(selectedTrack = track) }
                }
            }
        }
    }

    private fun startCreate() {
        // Whole minutes are enough; by default the activity just ended, so it started a moment ago.
        val now = Clock.System.now().toEpochMilliseconds().let { it - it % MINUTE_MILLIS }
        val form = ActivityForm()
        _state.update {
            it.copy(
                form = form.copy(startTime = now - (form.durationMinutes * MINUTE_MILLIS).toLong()),
                errorMessage = null
            )
        }
    }

    private fun startEdit(activityId: String) {
        val state = _state.value
        val activity = state.activities.firstOrNull { it.id == activityId }
            ?: state.selectedActivity?.takeIf { it.id == activityId }
            ?: return
        if (activity.userId != currentUserId) {
            _state.update { it.copy(errorMessage = NOT_OWNER_MESSAGE) }
            return
        }
        _state.update {
            it.copy(
                errorMessage = null,
                form = ActivityForm(
                    activityId = activity.id,
                    title = activity.title,
                    description = activity.description.orEmpty(),
                    activityType = activity.activityType,
                    distanceKm = activity.distanceMeters / 1000.0,
                    durationMinutes = activity.durationSeconds / 60.0,
                    elevationMeters = activity.elevationGainMeters,
                    startTime = activity.startTime,
                    isMeasured = !activity.isManualEntry
                )
            )
        }
    }

    private fun updateForm(transform: (ActivityForm) -> ActivityForm) {
        _state.update { state -> state.copy(form = state.form?.let(transform)) }
    }

    private fun submit(userId: String, teamId: String?) {
        val form = _state.value.form ?: return
        val state = _state.value
        val existing = form.activityId?.let { id ->
            state.activities.firstOrNull { it.id == id } ?: state.selectedActivity?.takeIf { it.id == id }
        }
        if (form.isEditing && existing == null) return
        if (existing != null && existing.userId != currentUserId) {
            _state.update { it.copy(errorMessage = NOT_OWNER_MESSAGE) }
            return
        }

        _state.update { it.copy(isSaving = true, errorMessage = null, success = null) }
        scope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val result = if (existing == null) {
                createManualActivityUseCase(
                    userId = userId,
                    teamId = teamId,
                    title = form.title,
                    description = form.description.ifBlank { null },
                    activityType = form.activityType,
                    distanceMeters = form.distanceKm * 1000.0,
                    durationSeconds = form.durationMinutes * 60.0,
                    elevationGainMeters = form.elevationMeters,
                    startTime = form.startTime
                )
            } else {
                val durationSeconds = if (form.isMeasured) existing.durationSeconds else form.durationMinutes * 60.0
                val startTime = if (form.isMeasured) existing.startTime else form.startTime
                updateActivityUseCase(
                    existing.copy(
                        title = form.title,
                        description = form.description.ifBlank { null },
                        activityType = form.activityType,
                        // Measured (GPS) values are not editable.
                        distanceMeters = if (form.isMeasured) existing.distanceMeters else form.distanceKm * 1000.0,
                        durationSeconds = durationSeconds,
                        elevationGainMeters = if (form.isMeasured) existing.elevationGainMeters else form.elevationMeters,
                        startTime = startTime,
                        endTime = if (form.isMeasured) existing.endTime else startTime + (durationSeconds * 1000).toLong(),
                        updatedAt = now
                    )
                )
            }
            result
                .onSuccess {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            success = if (existing == null) ActivitySuccess.CREATED else ActivitySuccess.UPDATED
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isSaving = false, errorMessage = error.message ?: "Saving failed") }
                }
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
                trackingActivityType = activityType
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
            _state.update { it.copy(isSaving = true, isTracking = false, errorMessage = null, success = null) }
            val endTime = Clock.System.now().toEpochMilliseconds()
            val result = recordGpsActivityUseCase(
                userId = userId,
                teamId = teamId,
                title = title.ifBlank { "${currentState.trackingActivityType.replaceFirstChar { c -> c.uppercase() }} Workout" },
                description = "Recorded with WANDR GPS Tracker",
                activityType = currentState.trackingActivityType,
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
                            isSaving = false,
                            success = ActivitySuccess.RECORDED,
                            liveTrackpoints = emptyList(),
                            liveDistanceMeters = 0.0,
                            liveDurationSeconds = 0.0
                        )
                    }
                },
                onFailure = { error ->
                    _state.update { s -> s.copy(isSaving = false, errorMessage = error.message ?: "Failed to save GPS activity") }
                }
            )
        }
    }

    private fun deleteActivity(activityId: String) {
        val activity = _state.value.activities.firstOrNull { it.id == activityId }
            ?: _state.value.selectedActivity?.takeIf { it.id == activityId }
        if (activity != null && activity.userId != currentUserId) {
            _state.update { it.copy(errorMessage = NOT_OWNER_MESSAGE) }
            return
        }
        scope.launch {
            _state.update { it.copy(isSaving = true) }
            deleteActivityUseCase(activityId).fold(
                onSuccess = { _state.update { s -> s.copy(isSaving = false, success = ActivitySuccess.DELETED) } },
                onFailure = { error ->
                    _state.update { s -> s.copy(isSaving = false, errorMessage = error.message ?: "Failed to delete activity") }
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

    private companion object {
        const val MINUTE_MILLIS = 60_000L
        const val NOT_OWNER_MESSAGE = "Only the owner can change this activity"
    }
}
