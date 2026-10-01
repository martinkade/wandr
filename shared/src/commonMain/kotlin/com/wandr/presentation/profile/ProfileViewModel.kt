package com.wandr.presentation.profile

import com.wandr.domain.model.Profile
import com.wandr.domain.usecase.GetProfileUseCase
import com.wandr.domain.usecase.RefreshProfileUseCase
import com.wandr.domain.usecase.RemoveAvatarUseCase
import com.wandr.domain.usecase.UpdateProfileUseCase
import com.wandr.domain.usecase.UploadAvatarUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val uploadAvatarUseCase: UploadAvatarUseCase,
    private val removeAvatarUseCase: RemoveAvatarUseCase,
    private val refreshProfileUseCase: RefreshProfileUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var cachedProfile: Profile? = null

    fun processIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LoadProfile -> loadProfile(intent.userId)
            is ProfileIntent.DisplayNameChanged -> _uiState.update { state ->
                state.copy(profile = state.profile?.copy(displayName = intent.name), hasUnsavedChanges = true)
            }
            is ProfileIntent.BioChanged -> _uiState.update { state ->
                state.copy(profile = state.profile?.copy(bio = intent.bio), hasUnsavedChanges = true)
            }
            is ProfileIntent.UploadAvatar -> updateAvatar { uploadAvatarUseCase(intent.userId, intent.jpegBytes) }
            is ProfileIntent.RemoveAvatar -> updateAvatar { removeAvatarUseCase(intent.userId) }
            is ProfileIntent.SaveProfile -> saveProfile()
            is ProfileIntent.DiscardChanges -> _uiState.update {
                it.copy(profile = cachedProfile ?: it.profile, hasUnsavedChanges = false)
            }
            is ProfileIntent.ClearMessages -> _uiState.update { it.copy(errorMessage = null, success = null) }
        }
    }

    private fun loadProfile(userId: String) {
        _uiState.update { it.copy(isLoading = it.profile == null) }
        observeJob?.cancel()
        observeJob = scope.launch {
            // The local cache is the source of truth for the UI; the remote pull only feeds the cache.
            launch { refreshProfileUseCase(userId) }
            getProfileUseCase(userId).collect { cached ->
                cachedProfile = cached
                _uiState.update { state ->
                    val draft = state.profile
                    val merged = if (state.hasUnsavedChanges && draft != null && cached != null) {
                        cached.copy(displayName = draft.displayName, bio = draft.bio)
                    } else cached
                    state.copy(profile = merged, isLoading = false)
                }
            }
        }
    }

    private fun saveProfile() {
        val currentProfile = _uiState.value.profile ?: return
        _uiState.update { it.copy(isSaving = true, errorMessage = null, success = null) }
        scope.launch {
            updateProfileUseCase(currentProfile)
                .onSuccess { updated ->
                    _uiState.update {
                        it.copy(profile = updated, isSaving = false, hasUnsavedChanges = false, success = ProfileSuccess.PROFILE_SAVED)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Save failed") }
                }
        }
    }

    private fun updateAvatar(action: suspend () -> Result<com.wandr.domain.model.Profile>) {
        _uiState.update { it.copy(isAvatarUpdating = true, errorMessage = null, success = null) }
        scope.launch {
            action()
                .onSuccess { updated ->
                    _uiState.update { state ->
                        // Keep the user's unsaved text edits, only take over the new avatar.
                        val draft = state.profile
                        val merged = if (state.hasUnsavedChanges && draft != null) {
                            updated.copy(displayName = draft.displayName, bio = draft.bio)
                        } else updated
                        state.copy(profile = merged, isAvatarUpdating = false, success = ProfileSuccess.AVATAR_UPDATED)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isAvatarUpdating = false, errorMessage = error.message ?: "Avatar update failed")
                    }
                }
        }
    }
}
