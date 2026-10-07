package com.wandr.presentation.teamdetails

import com.wandr.domain.error.AppError
import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamMember

enum class TeamDetailsSuccess { TEAM_SAVED, IMAGE_UPDATED }

data class TeamDetailsState(
    val team: Team? = null,
    val members: List<TeamMember> = emptyList(),
    /** Only the team owner and team admins may edit (matches the RLS update policy). */
    val canEdit: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isImageUpdating: Boolean = false,
    /** True while name/description were edited without saving; protects the draft from cache emissions. */
    val hasUnsavedChanges: Boolean = false,
    val error: AppError? = null,
    val success: TeamDetailsSuccess? = null
)
