package com.wandr.presentation.watch

import com.wandr.domain.error.AppError
import com.wandr.domain.error.asAppError
import com.wandr.domain.model.ActivityConflictException
import com.wandr.domain.model.ConflictResolution
import com.wandr.domain.usecase.ImportWatchWorkoutUseCase
import com.wandr.domain.watch.WatchWorkout
import com.wandr.domain.watch.WatchWorkoutInbox
import com.wandr.presentation.activity.ActivityConflict
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WatchImportState(
    /** The workout the conflict wizard is about; non-null together with [conflict]. */
    val conflictWorkout: WatchWorkout? = null,
    val conflict: ActivityConflict? = null,
    val isSaving: Boolean = false,
    /** How many workouts were imported since the last [WatchImportIntent.ClearMessages]. */
    val importedCount: Int = 0,
    val error: AppError? = null
)

sealed interface WatchImportIntent {
    /** Starts importing what arrives from the watch for this user. */
    data class Start(val userId: String, val teamId: String?) : WatchImportIntent
    data class ResolveConflict(val resolution: ConflictResolution) : WatchImportIntent
    object ClearMessages : WatchImportIntent
}

/**
 * Imports workouts that arrive from a watch. A workout overlapping existing activities waits for the user's choice
 * in the conflict wizard; the others follow afterwards.
 */
class WatchImportViewModel(
    private val inbox: WatchWorkoutInbox,
    private val importWatchWorkout: ImportWatchWorkoutUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(WatchImportState())
    val state: StateFlow<WatchImportState> = _state.asStateFlow()

    private var collectJob: Job? = null
    private var userId: String? = null
    private var teamId: String? = null
    private var busy = false

    /** Workouts that failed for another reason than a conflict; they are not retried until the next start. */
    private val failed = mutableSetOf<String>()

    fun processIntent(intent: WatchImportIntent) {
        when (intent) {
            is WatchImportIntent.Start -> start(intent.userId, intent.teamId)
            is WatchImportIntent.ResolveConflict -> resolve(intent.resolution)
            is WatchImportIntent.ClearMessages -> _state.update {
                it.copy(
                    importedCount = 0,
                    error = null
                )
            }
        }
    }

    private fun start(userId: String, teamId: String?) {
        this.userId = userId
        this.teamId = teamId
        failed.clear()
        collectJob?.cancel()
        collectJob = scope.launch { inbox.pending.collect { processNext() } }
    }

    private fun processNext() {
        if (busy || _state.value.conflict != null) return
        val user = userId ?: return
        val workout = inbox.pending.value.firstOrNull { it.id !in failed } ?: return
        busy = true
        scope.launch { import(user, workout, resolution = null) }
    }

    private suspend fun import(user: String, workout: WatchWorkout, resolution: ConflictResolution?) {
        _state.update { it.copy(isSaving = true) }
        importWatchWorkout(user, teamId, workout, resolution).fold(
            onSuccess = {
                inbox.handled(workout.id)
                _state.update {
                    it.copy(isSaving = false, conflict = null, conflictWorkout = null, importedCount = it.importedCount + 1)
                }
            },
            onFailure = { error ->
                if (error is ActivityConflictException) {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            conflictWorkout = workout,
                            // Not dismissible: the workout only exists on the watch and in the inbox.
                            conflict = ActivityConflict(error.conflicting, error.canTrim, dismissible = false)
                        )
                    }
                } else {
                    failed += workout.id
                    _state.update {
                        it.copy(
                            isSaving = false,
                            conflict = null,
                            conflictWorkout = null,
                            error = error.asAppError()
                        )
                    }
                }
            }
        )
        busy = false
        processNext()
    }

    private fun resolve(resolution: ConflictResolution) {
        val workout = _state.value.conflictWorkout ?: return
        val user = userId ?: return
        if (busy) return
        if (resolution == ConflictResolution.DISCARD) {
            inbox.handled(workout.id)
            _state.update { it.copy(conflict = null, conflictWorkout = null) }
            processNext()
            return
        }
        busy = true
        scope.launch { import(user, workout, resolution) }
    }
}
