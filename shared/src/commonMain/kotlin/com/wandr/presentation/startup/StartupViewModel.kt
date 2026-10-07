package com.wandr.presentation.startup

import com.wandr.domain.error.asAppError
import com.wandr.domain.usecase.InitializeAppUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StartupViewModel(
    private val initializeAppUseCase: InitializeAppUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _uiState = MutableStateFlow<StartupState>(StartupState.Loading)
    val uiState: StateFlow<StartupState> = _uiState.asStateFlow()

    private var job: Job? = null

    fun processIntent(intent: StartupIntent) {
        when (intent) {
            StartupIntent.Start -> if (job == null) run()
            StartupIntent.Retry -> run()
        }
    }

    private fun run() {
        job?.cancel()
        _uiState.value = StartupState.Loading
        job = scope.launch {
            initializeAppUseCase()
                .onSuccess { hasSession ->
                    _uiState.value = if (hasSession) StartupState.Authenticated else StartupState.Unauthenticated
                }
                .onFailure { error ->
                    _uiState.value = StartupState.Failed(error.asAppError())
                }
        }
    }
}
