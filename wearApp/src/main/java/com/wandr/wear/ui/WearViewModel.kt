package com.wandr.wear.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wandr.wear.WandrWearApplication
import com.wandr.wear.model.WorkoutPhase
import com.wandr.wear.tracking.WorkoutForegroundService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** State holder of the watch UI. Permissions are checked by the UI before [WearIntent.StartWorkout] is sent. */
class WearViewModel(application: Application) : AndroidViewModel(application) {
    private val graph = (application as WandrWearApplication).graph
    private val _state = MutableStateFlow(WearUiState())
    val state: StateFlow<WearUiState> = _state.asStateFlow()

    init {
        // Re-send workouts that have no data item (e.g. the watch was offline when the workout ended).
        viewModelScope.launch { graph.syncer.syncAll() }
        viewModelScope.launch {
            graph.tracker.state.collect { live ->
                _state.update { current ->
                    val screen = if (live.phase == WorkoutPhase.ACTIVE || live.phase == WorkoutPhase.PAUSED) WearScreen.Workout else current.screen
                    current.copy(live = live, screen = screen)
                }
            }
        }
        viewModelScope.launch {
            combine(graph.outbox.entries, graph.syncer.sent) { entries, sent -> entries to sent }.collect { (entries, sent) ->
                _state.update { current ->
                    val summary = current.screen as? WearScreen.Summary ?: return@update current
                    current.copy(summarySendState = graph.syncer.stateOf(summary.workout.id, entries, sent))
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                if (_state.value.live.phase == WorkoutPhase.ACTIVE) graph.tracker.tick()
            }
        }
    }

    fun onIntent(intent: WearIntent) {
        when (intent) {
            is WearIntent.SelectType -> _state.update { it.copy(screen = WearScreen.Start(intent.type), startFailed = false) }
            WearIntent.Back -> _state.update { if (it.screen is WearScreen.Start) it.copy(screen = WearScreen.TypePicker) else it }
            WearIntent.StartWorkout -> startWorkout()
            WearIntent.Pause -> launchTracker { graph.tracker.pause() }
            WearIntent.Resume -> launchTracker { graph.tracker.resume() }
            WearIntent.Stop -> stopWorkout()
            WearIntent.Done -> _state.update { it.copy(screen = WearScreen.TypePicker) }
        }
    }

    private fun startWorkout() {
        val type = (_state.value.screen as? WearScreen.Start)?.type ?: return
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, startFailed = false) }
        viewModelScope.launch {
            try {
                WorkoutForegroundService.start(getApplication())
                graph.tracker.start(type)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(startFailed = true) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun stopWorkout() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val workout = graph.tracker.stop()
                graph.outbox.add(workout)
                _state.update { it.copy(screen = WearScreen.Summary(workout), summarySendState = com.wandr.wear.model.SendState.PENDING) }
                graph.syncer.send(workout.id)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The session could not be ended cleanly; stay on the live screen so the user can retry.
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun launchTracker(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }
}
