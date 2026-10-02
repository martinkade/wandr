package com.wandr.wear.ui

import com.wandr.domain.watch.WatchWorkout
import com.wandr.wear.model.LiveWorkoutState
import com.wandr.wear.model.SendState
import com.wandr.wear.model.WearActivityType

sealed interface WearScreen {
    data object TypePicker : WearScreen
    data class Start(val type: WearActivityType) : WearScreen
    data object Workout : WearScreen
    data class Summary(val workout: WatchWorkout) : WearScreen
}

data class WearUiState(
    val screen: WearScreen = WearScreen.TypePicker,
    val live: LiveWorkoutState = LiveWorkoutState(),
    val summarySendState: SendState = SendState.PENDING,
    val startFailed: Boolean = false,
    val busy: Boolean = false
)

sealed interface WearIntent {
    data class SelectType(val type: WearActivityType) : WearIntent
    data object Back : WearIntent
    data object StartWorkout : WearIntent
    data object Pause : WearIntent
    data object Resume : WearIntent
    data object Stop : WearIntent
    data object Done : WearIntent
}
