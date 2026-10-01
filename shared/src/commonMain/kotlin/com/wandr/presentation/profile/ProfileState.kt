package com.wandr.presentation.profile

import com.wandr.domain.model.Profile

data class ProfileState(
    val profile: Profile? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAvatarUpdating: Boolean = false,
    /** True while the user has edited name/bio without saving; protects the draft from cache emissions. */
    val hasUnsavedChanges: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
