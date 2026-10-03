package com.wandr.presentation.team

sealed interface TeamIntent {
    data class LoadUserTeams(val userId: String) : TeamIntent
    data class SelectTeam(val teamId: String) : TeamIntent
    data class CreateTeamNameChanged(val name: String) : TeamIntent
    data class CreateTeamDescriptionChanged(val description: String) : TeamIntent
    data class JoinInviteCodeChanged(val code: String) : TeamIntent
    data class SubmitCreateTeam(val creatorId: String) : TeamIntent
    data class SubmitJoinTeam(val userId: String) : TeamIntent
    /** [teamIds] is the new priority order, highest first. */
    data class ReorderTeams(val userId: String, val teamIds: List<String>) : TeamIntent
    data class GenerateQRCode(val inviteCode: String) : TeamIntent
    data object ClearMessages : TeamIntent
}
