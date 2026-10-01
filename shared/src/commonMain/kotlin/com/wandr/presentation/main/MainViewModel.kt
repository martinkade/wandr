package com.wandr.presentation.main

import com.wandr.data.sync.SyncManager
import com.wandr.domain.model.SystemRole
import com.wandr.domain.repository.AuthRepository
import com.wandr.domain.usecase.GetProfileUseCase
import com.wandr.domain.usecase.GetUserTeamsUseCase
import com.wandr.domain.usecase.LogoutUseCase
import com.wandr.domain.usecase.RefreshProfileUseCase
import com.wandr.domain.usecase.RefreshUserTeamsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Resolves the signed-in user, their role and primary team for the main tabs. */
class MainViewModel(
    private val authRepository: AuthRepository,
    private val getUserTeamsUseCase: GetUserTeamsUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val syncManager: SyncManager,
    private val getProfileUseCase: GetProfileUseCase,
    private val refreshProfileUseCase: RefreshProfileUseCase,
    private val refreshUserTeamsUseCase: RefreshUserTeamsUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _uiState = MutableStateFlow(MainState())
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun load() {
        syncManager.triggerSync() // pushes edits made offline (e.g. profile changes)
        observeJob?.cancel()
        observeJob = scope.launch {
            val userId = authRepository.currentSession().first()?.user?.id
            _uiState.update { it.copy(userId = userId, isLoading = false) }
            if (userId == null) return@launch

            // The local cache drives the UI; remote pulls only feed it and may fail while offline.
            launch { refreshProfileUseCase(userId) }
            launch { refreshUserTeamsUseCase(userId) }
            launch {
                getProfileUseCase(userId).collect { profile ->
                    _uiState.update { it.copy(isManager = profile?.systemRole == SystemRole.MANAGER) }
                }
            }
            launch {
                getUserTeamsUseCase(userId).collect { teams ->
                    _uiState.update { it.copy(teamId = teams.firstOrNull()?.id) }
                }
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        scope.launch {
            logoutUseCase()
            onDone()
        }
    }
}
