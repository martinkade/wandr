package com.wandr.presentation.startup

sealed interface StartupState {
    data object Loading : StartupState
    data object Authenticated : StartupState
    data object Unauthenticated : StartupState
    data class Failed(val message: String) : StartupState
}
