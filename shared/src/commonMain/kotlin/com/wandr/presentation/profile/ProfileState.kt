package com.wandr.presentation.profile

import com.wandr.domain.model.Profile

enum class ProfileSuccess { PROFILE_SAVED, AVATAR_UPDATED }

data class ProfileState(
    val profile: Profile? = null,
    /** Number of recorded activities, shown in the profile stats. */
    val activityCount: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAvatarUpdating: Boolean = false,
    /** True while the user has edited name/bio without saving; protects the draft from cache emissions. */
    val hasUnsavedChanges: Boolean = false,
    val errorMessage: String? = null,
    /** What succeeded last; the UI maps it to a localized message. Cleared via [ProfileIntent.ClearMessages]. */
    val success: ProfileSuccess? = null
)
