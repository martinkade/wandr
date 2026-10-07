package com.wandr.presentation.teamdetails

import com.wandr.domain.error.asAppError
import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamImageKind
import com.wandr.domain.model.TeamMember
import com.wandr.domain.model.TeamRole
import com.wandr.domain.usecase.GetTeamMembersUseCase
import com.wandr.domain.usecase.GetTeamUseCase
import com.wandr.domain.usecase.RefreshTeamDetailsUseCase
import com.wandr.domain.usecase.RemoveTeamImageUseCase
import com.wandr.domain.usecase.SetTeamImageUseCase
import com.wandr.domain.usecase.UpdateTeamUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TeamDetailsViewModel(
    private val getTeamUseCase: GetTeamUseCase,
    private val getTeamMembersUseCase: GetTeamMembersUseCase,
    private val refreshTeamDetailsUseCase: RefreshTeamDetailsUseCase,
    private val updateTeamUseCase: UpdateTeamUseCase,
    private val setTeamImageUseCase: SetTeamImageUseCase,
    private val removeTeamImageUseCase: RemoveTeamImageUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _uiState = MutableStateFlow(TeamDetailsState())
    val uiState: StateFlow<TeamDetailsState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var cachedTeam: Team? = null
    private var userId: String? = null

    fun processIntent(intent: TeamDetailsIntent) {
        when (intent) {
            is TeamDetailsIntent.Load -> load(intent.teamId, intent.userId)
            is TeamDetailsIntent.NameChanged -> _uiState.update { state ->
                state.copy(team = state.team?.copy(name = intent.name), hasUnsavedChanges = true)
            }
            is TeamDetailsIntent.DescriptionChanged -> _uiState.update { state ->
                state.copy(team = state.team?.copy(description = intent.description), hasUnsavedChanges = true)
            }
            is TeamDetailsIntent.UploadAvatar -> updateImage { id -> setTeamImageUseCase(id, TeamImageKind.AVATAR, intent.jpegBytes) }
            is TeamDetailsIntent.UploadCover -> updateImage { id -> setTeamImageUseCase(id, TeamImageKind.COVER, intent.jpegBytes) }
            is TeamDetailsIntent.RemoveAvatar -> updateImage { id -> removeTeamImageUseCase(id, TeamImageKind.AVATAR) }
            is TeamDetailsIntent.RemoveCover -> updateImage { id -> removeTeamImageUseCase(id, TeamImageKind.COVER) }
            is TeamDetailsIntent.Save -> save()
            is TeamDetailsIntent.DiscardChanges -> _uiState.update {
                it.copy(team = cachedTeam ?: it.team, hasUnsavedChanges = false)
            }
            is TeamDetailsIntent.ClearMessages -> _uiState.update {
                it.copy(
                    error = null,
                    success = null
                )
            }
        }
    }

    private fun load(teamId: String, userId: String) {
        this.userId = userId
        // Switching to another team must not show the previous one while loading.
        if (_uiState.value.team?.id != teamId) {
            cachedTeam = null
            _uiState.value = TeamDetailsState(isLoading = true)
        }
        observeJob?.cancel()
        observeJob = scope.launch {
            // The local cache drives the UI; the remote pull only feeds it (and may fail while offline).
            launch { refreshTeamDetailsUseCase(teamId) }
            launch {
                getTeamUseCase(teamId).collect { cached ->
                    cachedTeam = cached
                    _uiState.update { state ->
                        val draft = state.team
                        val merged = if (state.hasUnsavedChanges && draft != null && cached != null) {
                            cached.copy(name = draft.name, description = draft.description)
                        } else cached
                        state.copy(team = merged, isLoading = false, canEdit = canEdit(merged, state.members))
                    }
                }
            }
            getTeamMembersUseCase(teamId).collect { members ->
                _uiState.update { it.copy(members = members, canEdit = canEdit(it.team, members)) }
            }
        }
    }

    private fun canEdit(team: Team?, members: List<TeamMember>): Boolean {
        val me = userId ?: return false
        return team?.createdBy == me || members.any { it.userId == me && it.role == TeamRole.ADMIN }
    }

    private fun save() {
        val team = _uiState.value.team ?: return
        _uiState.update { it.copy(isSaving = true, error = null, success = null) }
        scope.launch {
            updateTeamUseCase(team)
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(team = updated, isSaving = false, hasUnsavedChanges = false, success = TeamDetailsSuccess.TEAM_SAVED)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, error = error.asAppError()) }
                }
        }
    }

    private fun updateImage(action: suspend (teamId: String) -> Result<Team>) {
        val teamId = _uiState.value.team?.id ?: return
        _uiState.update { it.copy(isImageUpdating = true, error = null, success = null) }
        scope.launch {
            action(teamId)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        // Keep unsaved text edits, only take over the new image.
                        val draft = state.team
                        val merged = if (state.hasUnsavedChanges && draft != null) {
                            updated.copy(name = draft.name, description = draft.description)
                        } else updated
                        state.copy(team = merged, isImageUpdating = false, success = TeamDetailsSuccess.IMAGE_UPDATED)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isImageUpdating = false, error = error.asAppError()) }
                }
        }
    }
}
