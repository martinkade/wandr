package com.wandr.presentation.team

import com.wandr.domain.repository.TeamRepository
import com.wandr.domain.usecase.CreateTeamUseCase
import com.wandr.domain.usecase.GetUserTeamsUseCase
import com.wandr.domain.usecase.JoinTeamViaInviteUseCase
import com.wandr.domain.usecase.RefreshUserTeamsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeamViewModel(
    private val getUserTeamsUseCase: GetUserTeamsUseCase,
    private val createTeamUseCase: CreateTeamUseCase,
    private val joinTeamViaInviteUseCase: JoinTeamViaInviteUseCase,
    private val refreshUserTeamsUseCase: RefreshUserTeamsUseCase,
    private val teamRepository: TeamRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _uiState = MutableStateFlow(TeamState())
    val uiState: StateFlow<TeamState> = _uiState.asStateFlow()

    fun processIntent(intent: TeamIntent) {
        when (intent) {
            is TeamIntent.LoadUserTeams -> loadUserTeams(intent.userId)
            is TeamIntent.SelectTeam -> selectTeam(intent.teamId)
            is TeamIntent.CreateTeamNameChanged -> _uiState.update { it.copy(createTeamName = intent.name) }
            is TeamIntent.CreateTeamDescriptionChanged -> _uiState.update { it.copy(createTeamDescription = intent.description) }
            is TeamIntent.JoinInviteCodeChanged -> _uiState.update { it.copy(joinInviteCode = intent.code) }
            is TeamIntent.SubmitCreateTeam -> createTeam(intent.creatorId)
            is TeamIntent.SubmitJoinTeam -> joinTeam(intent.userId)
            is TeamIntent.GenerateQRCode -> generateQRCodeUrl(intent.inviteCode)
            is TeamIntent.ClearMessages -> _uiState.update { it.copy(errorMessage = null, successMessage = null) }
        }
    }

    private fun loadUserTeams(userId: String) {
        _uiState.update { it.copy(isLoading = true) }
        scope.launch {
            // The local cache drives the UI; the remote pull only feeds it (and may fail while offline).
            launch { refreshUserTeamsUseCase(userId) }
            getUserTeamsUseCase(userId).collect { teams ->
                _uiState.update { it.copy(teams = teams, isLoading = false) }
            }
        }
    }

    private fun selectTeam(teamId: String) {
        _uiState.update { it.copy(isLoading = true) }
        scope.launch {
            teamRepository.getTeamById(teamId).collect { team ->
                _uiState.update { it.copy(selectedTeam = team, isLoading = false) }
            }
        }
    }

    private fun createTeam(creatorId: String) {
        val name = _uiState.value.createTeamName
        val description = _uiState.value.createTeamDescription
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        scope.launch {
            createTeamUseCase(name, description, creatorId)
                .onSuccess { team ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Team created successfully!",
                            createTeamName = "",
                            createTeamDescription = ""
                        ) 
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Failed to create team") }
                }
        }
    }

    private fun joinTeam(userId: String) {
        val code = _uiState.value.joinInviteCode
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        scope.launch {
            joinTeamViaInviteUseCase(code, userId)
                .onSuccess { team ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            successMessage = "Joined team ${team.name}!",
                            joinInviteCode = ""
                        ) 
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Failed to join team") }
                }
        }
    }

    private fun generateQRCodeUrl(inviteCode: String) {
        scope.launch {
            val url = teamRepository.generateInviteUrl(inviteCode)
            _uiState.update { it.copy(qrInviteUrl = url) }
        }
    }
}
