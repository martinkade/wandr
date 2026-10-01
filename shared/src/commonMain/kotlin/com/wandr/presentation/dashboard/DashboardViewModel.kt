package com.wandr.presentation.dashboard

import com.wandr.domain.repository.AuthRepository
import com.wandr.domain.usecase.GetUserTeamsUseCase
import com.wandr.domain.usecase.LogoutUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Resolves the signed-in user and their primary team for the dashboard tabs. */
class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val getUserTeamsUseCase: GetUserTeamsUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            val userId = authRepository.currentSession().first()?.user?.id
            val teamId = userId?.let { runCatching { getUserTeamsUseCase(it).first().firstOrNull()?.id }.getOrNull() }
            _uiState.value = DashboardState(userId = userId, teamId = teamId, isLoading = false)
        }
    }

    fun logout(onDone: () -> Unit) {
        scope.launch {
            logoutUseCase()
            onDone()
        }
    }
}
