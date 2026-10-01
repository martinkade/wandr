package com.wandr.presentation.profile

import com.wandr.domain.usecase.GetProfileUseCase
import com.wandr.domain.usecase.UpdateProfileUseCase
import com.wandr.domain.usecase.UploadAvatarUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val uploadAvatarUseCase: UploadAvatarUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    fun processIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LoadProfile -> loadProfile(intent.userId)
            is ProfileIntent.DisplayNameChanged -> {
                _uiState.update { state ->
                    state.copy(profile = state.profile?.copy(displayName = intent.name))
                }
            }
            is ProfileIntent.BioChanged -> {
                _uiState.update { state ->
                    state.copy(profile = state.profile?.copy(bio = intent.bio))
                }
            }
            is ProfileIntent.UploadAvatar -> uploadAvatar(intent.userId, intent.bytes, intent.fileName)
            is ProfileIntent.SaveProfile -> saveProfile()
        }
    }

    private fun loadProfile(userId: String) {
        _uiState.update { it.copy(isLoading = true) }
        scope.launch {
            getProfileUseCase(userId).collect { profile ->
                _uiState.update { it.copy(profile = profile, isLoading = false) }
            }
        }
    }

    private fun saveProfile() {
        val currentProfile = _uiState.value.profile ?: return
        _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
        scope.launch {
            updateProfileUseCase(currentProfile)
                .onSuccess { updated ->
                    _uiState.update { it.copy(profile = updated, isSaving = false, successMessage = "Profile saved!") }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Save failed") }
                }
        }
    }

    private fun uploadAvatar(userId: String, bytes: ByteArray, fileName: String) {
        _uiState.update { it.copy(isSaving = true) }
        scope.launch {
            uploadAvatarUseCase(userId, bytes, fileName)
                .onSuccess { avatarUrl ->
                    val updated = _uiState.value.profile?.copy(avatarUrl = avatarUrl)
                    if (updated != null) {
                        updateProfileUseCase(updated)
                    }
                    _uiState.update { it.copy(isSaving = false, successMessage = "Avatar updated!") }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.message ?: "Avatar upload failed") }
                }
        }
    }
}
