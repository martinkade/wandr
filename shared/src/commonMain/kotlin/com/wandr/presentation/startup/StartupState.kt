package com.wandr.presentation.startup

import com.wandr.domain.error.AppError

sealed interface StartupState {
    data object Loading : StartupState
    data object Authenticated : StartupState
    data object Unauthenticated : StartupState

    /** Starting failed; [error] says why (the local storage could not be opened, ...). */
    data class Failed(val error: AppError) : StartupState
}
