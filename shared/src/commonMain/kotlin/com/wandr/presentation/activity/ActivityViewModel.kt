package com.wandr.presentation.activity

import com.wandr.domain.geo.ElevationGainTracker
import com.wandr.domain.geo.GeoMath
import com.wandr.domain.geo.RecordingMetrics
import com.wandr.domain.geo.RecordingPolicy
import com.wandr.domain.model.Activity
import com.wandr.domain.model.ActivityConflictException
import com.wandr.domain.model.ConflictResolution
import com.wandr.domain.model.SocialCounts
import com.wandr.domain.model.SocialEntityType
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.usecase.CreateManualActivityUseCase
import com.wandr.domain.usecase.DeleteActivityUseCase
import com.wandr.domain.usecase.GetActivityTrackUseCase
import com.wandr.domain.usecase.GetProfilesUseCase
import com.wandr.domain.usecase.GetSocialCountsUseCase
import com.wandr.domain.usecase.SetLikeUseCase
import com.wandr.domain.usecase.GetActivityUseCase
import com.wandr.domain.usecase.GetTeamActivitiesUseCase
import com.wandr.domain.usecase.GetUserActivitiesUseCase
import com.wandr.domain.usecase.RecordGpsActivityUseCase
import com.wandr.domain.usecase.RefreshActivitiesUseCase
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
    private val refreshActivitiesUseCase: RefreshActivitiesUseCase,
    private val getSocialCountsUseCase: GetSocialCountsUseCase,
    private val setLikeUseCase: SetLikeUseCase,
    private val getProfilesUseCase: GetProfilesUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    /** The clock of the recording (milliseconds); replaced in tests. The recording clock itself is advanced by [ActivityIntent.Tick]. */
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private val _state = MutableStateFlow(ActivityState())
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    private var trackingStartTime: Long = 0

    // Live recording bookkeeping that is not part of the state
    private var lastTickAt: Long = 0
    private var lastSplitSeconds = 0.0
    private var skipDistanceOfNextPoint = false
    private val elevationTracker = ElevationGainTracker()
    private var listJob: Job? = null
    private var selectJob: Job? = null
    private var currentUserId: String? = null
    private var countsJob: Job? = null
    private var authorsJob: Job? = null
    private var countedIds: List<String> = emptyList()
    private var observedAuthors: List<String> = emptyList()

    /** A save that was held back by a time conflict, waiting for the user's resolution. */
    private var pendingSave: PendingSave? = null

    private class PendingSave(
        val success: ActivitySuccess,
        val isRecording: Boolean,
        val save: suspend (ConflictResolution?) -> Result<Activity>
    )

    fun processIntent(intent: ActivityIntent) {
        when (intent) {
            is ActivityIntent.LoadUserActivities -> loadUserActivities(intent.userId)
            is ActivityIntent.LoadTeamActivities -> loadTeamActivities(intent.teamId, intent.userId)
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
            is ActivityIntent.ShowMapChanged -> updateForm { it.copy(showMap = intent.show) }
            is ActivityIntent.ToggleActivityLike -> toggleActivityLike(intent.activityId)
            is ActivityIntent.SubmitForm -> submit(intent.userId, intent.teamId)
            is ActivityIntent.DiscardForm -> _state.update { it.copy(form = null) }
            is ActivityIntent.StartGpsTracking -> startGpsTracking(intent.activityType)
            is ActivityIntent.AddTrackpoint -> addTrackpoint(intent.trackpoint)
            is ActivityIntent.GpsFixChanged -> _state.update { it.copy(gpsAccuracyMeters = intent.accuracyMeters) }
            is ActivityIntent.Tick -> onTick(intent.nowMillis)
            is ActivityIntent.DiscardRecording -> discardRecording()
            is ActivityIntent.PauseGpsTracking -> if (_state.value.isTracking) _state.update { it.copy(isPaused = true) }
            is ActivityIntent.ResumeGpsTracking -> if (_state.value.isTracking && _state.value.isPaused) {
                // The first point after a break is far from the last one; the way in between is not part of the activity.
                skipDistanceOfNextPoint = true
                _state.update { it.copy(isPaused = false) }
            }
            is ActivityIntent.StopAndSaveGpsTracking -> stopAndSaveGpsTracking(intent.userId, intent.teamId, intent.title)
            is ActivityIntent.ResolveConflict -> resolveConflict(intent.resolution)
            is ActivityIntent.DismissConflict -> dismissConflict()
            is ActivityIntent.DeleteActivity -> deleteActivity(intent.activityId)
            is ActivityIntent.ClearMessages -> _state.update { it.copy(errorMessage = null, success = null) }
        }
    }

    private fun loadUserActivities(userId: String) {
        currentUserId = userId
        _state.update { it.copy(isLoading = it.activities.isEmpty(), canEdit = it.selectedActivity?.userId == userId) }
        listJob?.cancel()
        scope.launch { refreshActivitiesUseCase.user(userId) } // activities from other devices; failures (offline) are fine
        listJob = scope.launch {
            getUserActivitiesUseCase(userId).collect { list ->
                _state.update { it.copy(activities = list, isLoading = false) }
                onListChanged(list)
            }
        }
    }

    private fun loadTeamActivities(teamId: String, userId: String) {
        currentUserId = userId
        listJob?.cancel()
        scope.launch { refreshActivitiesUseCase.team(teamId, userId) } // the feed also holds what teammates recorded
        listJob = scope.launch {
            _state.update { it.copy(isLoading = true) }
            getTeamActivitiesUseCase(teamId).collect { list ->
                _state.update { it.copy(activities = list, isLoading = false) }
                onListChanged(list)
            }
        }
    }

    /** Fetches what the cards need besides the activity itself: social counts and authors, each in one call. */
    private fun onListChanged(list: List<Activity>) {
        val ids = list.map { it.id }.take(MAX_FEED_ITEMS)
        // Only when the list has other items than before; edits of an item do not change its likes.
        if (ids != countedIds) {
            countedIds = ids
            countsJob?.cancel()
            countsJob = scope.launch {
                getSocialCountsUseCase(SocialEntityType.ACTIVITY, ids)
                    .onSuccess { counts -> _state.update { it.copy(socialCounts = it.socialCounts + counts) } }
            }
        }
        val authorIds = list.map { it.userId }.distinct()
        if (authorIds != observedAuthors) {
            observedAuthors = authorIds
            authorsJob?.cancel()
            authorsJob = scope.launch {
                getProfilesUseCase(authorIds).collect { profiles ->
                    _state.update { it.copy(authors = profiles.associateBy { p -> p.id }) }
                }
            }
        }
    }

    private fun toggleActivityLike(activityId: String) {
        val userId = currentUserId ?: return
        val before = _state.value.socialCounts[activityId] ?: SocialCounts()
        val liked = !before.likedByMe
        val after = before.copy(likeCount = (before.likeCount + if (liked) 1 else -1).coerceAtLeast(0), likedByMe = liked)
        _state.update { it.copy(socialCounts = it.socialCounts + (activityId to after)) }
        scope.launch {
            setLikeUseCase(SocialEntityType.ACTIVITY, activityId, userId, liked).onFailure { error ->
                _state.update { it.copy(socialCounts = it.socialCounts + (activityId to before), errorMessage = error.message ?: "Could not save the like") }
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
                    isMeasured = !activity.isManualEntry,
                    showMap = activity.showMap,
                    hasRoute = activity.polyline != null
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

        val now = Clock.System.now().toEpochMilliseconds()
        val pending = if (existing == null) {
            PendingSave(ActivitySuccess.CREATED, isRecording = false) { resolution ->
                createManualActivityUseCase(
                    userId = userId,
                    teamId = teamId,
                    title = form.title,
                    description = form.description.ifBlank { null },
                    activityType = form.activityType,
                    distanceMeters = form.distanceKm * 1000.0,
                    durationSeconds = form.durationMinutes * 60.0,
                    elevationGainMeters = form.elevationMeters,
                    startTime = form.startTime,
                    resolution = resolution
                )
            }
        } else {
            val durationSeconds = if (form.isMeasured) existing.durationSeconds else form.durationMinutes * 60.0
            val startTime = if (form.isMeasured) existing.startTime else form.startTime
            val updated = existing.copy(
                title = form.title,
                description = form.description.ifBlank { null },
                activityType = form.activityType,
                // Measured (GPS) values are not editable.
                distanceMeters = if (form.isMeasured) existing.distanceMeters else form.distanceKm * 1000.0,
                durationSeconds = durationSeconds,
                elevationGainMeters = if (form.isMeasured) existing.elevationGainMeters else form.elevationMeters,
                startTime = startTime,
                endTime = if (form.isMeasured) existing.endTime else startTime + (durationSeconds * 1000).toLong(),
                showMap = form.showMap,
                updatedAt = now
            )
            // Overlaps that already exist must not block edits that leave the time range alone.
            val timeChanged = updated.startTime != existing.startTime || updated.endTime != existing.endTime
            PendingSave(ActivitySuccess.UPDATED, isRecording = false) { resolution ->
                updateActivityUseCase(updated, checkConflicts = timeChanged, resolution = resolution)
            }
        }
        runSave(pending, resolution = null)
    }

    /** Runs [pending]; a time conflict opens the wizard instead of saving. */
    private fun runSave(pending: PendingSave, resolution: ConflictResolution?) {
        _state.update { it.copy(isSaving = true, errorMessage = null, success = null) }
        scope.launch {
            pending.save(resolution).fold(
                onSuccess = {
                    pendingSave = null
                    val success = when (resolution) {
                        ConflictResolution.MERGE -> ActivitySuccess.MERGED
                        ConflictResolution.TRIM -> ActivitySuccess.TRIMMED
                        else -> pending.success
                    }
                    _state.update { it.finishSave(pending, success) }
                },
                onFailure = { error ->
                    if (error is ActivityConflictException) {
                        pendingSave = pending
                        _state.update {
                            it.copy(
                                isSaving = false,
                                conflict = ActivityConflict(
                                    conflicting = error.conflicting,
                                    canTrim = error.canTrim,
                                    // A finished recording cannot be dismissed without losing it.
                                    dismissible = !pending.isRecording
                                )
                            )
                        }
                    } else {
                        pendingSave = null
                        _state.update { it.copy(isSaving = false, conflict = null, errorMessage = error.message ?: "Saving failed") }
                    }
                }
            )
        }
    }

    private fun ActivityState.finishSave(pending: PendingSave, success: ActivitySuccess): ActivityState = copy(
        isSaving = false,
        conflict = null,
        success = success,
        liveTrackpoints = if (pending.isRecording) emptyList() else liveTrackpoints,
        liveDistanceMeters = if (pending.isRecording) 0.0 else liveDistanceMeters,
        liveDurationSeconds = if (pending.isRecording) 0.0 else liveDurationSeconds,
        liveElevationGainMeters = if (pending.isRecording) 0.0 else liveElevationGainMeters,
        liveCurrentPaceSecondsPerKm = if (pending.isRecording) null else liveCurrentPaceSecondsPerKm,
        liveSplitsSeconds = if (pending.isRecording) emptyList() else liveSplitsSeconds
    )

    private fun resolveConflict(resolution: ConflictResolution) {
        val pending = pendingSave ?: return
        if (resolution == ConflictResolution.DISCARD) {
            // The existing activities stay; the new/edited one is dropped (for a recording: the recording).
            pendingSave = null
            _state.update { it.finishSave(pending, ActivitySuccess.DISCARDED) }
        } else {
            runSave(pending, resolution)
        }
    }

    private fun dismissConflict() {
        if (_state.value.conflict?.dismissible != true) return
        pendingSave = null
        _state.update { it.copy(conflict = null) }
    }

    private fun startGpsTracking(activityType: String) {
        trackingStartTime = nowMillis()
        lastTickAt = trackingStartTime
        lastSplitSeconds = 0.0
        skipDistanceOfNextPoint = false
        elevationTracker.reset()
        _state.update {
            it.copy(
                isTracking = true,
                isPaused = false,
                liveDistanceMeters = 0.0,
                liveDurationSeconds = 0.0,
                liveElevationGainMeters = 0.0,
                liveTrackpoints = emptyList(),
                liveCurrentPaceSecondsPerKm = null,
                liveSplitsSeconds = emptyList(),
                trackingActivityType = activityType
            )
        }
    }

    private fun onTick(now: Long) {
        val state = _state.value
        if (!state.isTracking) return
        val delta = (now - lastTickAt).coerceAtLeast(0)
        lastTickAt = now
        if (state.isPaused || delta == 0L) return
        _state.update { it.copy(liveDurationSeconds = it.liveDurationSeconds + delta / 1000.0) }
    }

    private fun addTrackpoint(trackpoint: GpsTrackpoint) {
        val s = _state.value
        if (!s.isTracking || s.isPaused) return
        val last = s.liveTrackpoints.lastOrNull()
        // Fast sports record every second, hiking every 3 seconds; providers may deliver more often than requested.
        if (last != null && trackpoint.timestamp - last.timestamp < RecordingPolicy.minGapMillis(s.trackingActivityType)) return
        val gain = elevationTracker.add(trackpoint.altitudeMeters)
        val updatedPoints = s.liveTrackpoints + trackpoint
        val addedDistance = if (last == null || skipDistanceOfNextPoint) 0.0 else {
            GeoMath.distanceMeters(last.latitude, last.longitude, trackpoint.latitude, trackpoint.longitude)
        }
        skipDistanceOfNextPoint = false
        val distance = s.liveDistanceMeters + addedDistance

        // A split is the time of each full kilometer.
        val splits = s.liveSplitsSeconds.toMutableList()
        while (splits.size < (distance / METERS_PER_KM).toInt()) {
            splits += (s.liveDurationSeconds - lastSplitSeconds).coerceAtLeast(0.0)
            lastSplitSeconds = s.liveDurationSeconds
        }

        _state.update {
            it.copy(
                liveTrackpoints = updatedPoints,
                liveDistanceMeters = distance,
                liveElevationGainMeters = it.liveElevationGainMeters + gain,
                liveCurrentPaceSecondsPerKm = RecordingMetrics.recentPaceSecondsPerKm(updatedPoints),
                liveSplitsSeconds = splits
            )
        }
    }

    private fun discardRecording() {
        _state.update {
            it.copy(
                isTracking = false,
                isPaused = false,
                liveTrackpoints = emptyList(),
                liveDistanceMeters = 0.0,
                liveDurationSeconds = 0.0,
                liveElevationGainMeters = 0.0,
                liveCurrentPaceSecondsPerKm = null,
                liveSplitsSeconds = emptyList()
            )
        }
    }

    private fun stopAndSaveGpsTracking(userId: String, teamId: String?, title: String) {
        val currentState = _state.value
        _state.update { it.copy(isTracking = false) }
        val endTime = nowMillis()
        val startTime = trackingStartTime
        val pending = PendingSave(ActivitySuccess.RECORDED, isRecording = true) { resolution ->
            recordGpsActivityUseCase(
                userId = userId,
                teamId = teamId,
                title = title.ifBlank { "${currentState.trackingActivityType.replaceFirstChar { c -> c.uppercase() }} Workout" },
                description = "Recorded with WANDR GPS Tracker",
                activityType = currentState.trackingActivityType,
                distanceMeters = currentState.liveDistanceMeters,
                durationSeconds = currentState.liveDurationSeconds,
                elevationGainMeters = currentState.liveElevationGainMeters,
                startTime = startTime,
                endTime = endTime,
                trackpoints = currentState.liveTrackpoints,
                resolution = resolution
            )
        }
        runSave(pending, resolution = null)
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

    private companion object {
        const val MINUTE_MILLIS = 60_000L
        const val METERS_PER_KM = 1_000.0
        /** The feed page size; counts are requested for at most this many cards. */
        const val MAX_FEED_ITEMS = 100
        const val NOT_OWNER_MESSAGE = "Only the owner can change this activity"
    }
}
