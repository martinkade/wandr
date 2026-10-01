package com.wandr.presentation.team

import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamMember

data class TeamState(
    val teams: List<Team> = emptyList(),
    val selectedTeam: Team? = null,
    val members: List<TeamMember> = emptyList(),
    val createTeamName: String = "",
    val createTeamDescription: String = "",
    val joinInviteCode: String = "",
    val qrInviteUrl: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
