package com.wandr.presentation.startup

sealed interface StartupIntent {
    data object Start : StartupIntent
    data object Retry : StartupIntent
}
