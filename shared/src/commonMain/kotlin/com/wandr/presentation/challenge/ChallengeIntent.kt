package com.wandr.presentation.challenge

sealed interface ChallengeIntent {
    data class LoadTeamChallenges(val teamId: String) : ChallengeIntent
    data class SelectChallenge(val challengeId: String, val teamId: String) : ChallengeIntent
    data class TitleChanged(val title: String) : ChallengeIntent
    data class DescriptionChanged(val description: String) : ChallengeIntent
    data class TargetValueChanged(val value: Double) : ChallengeIntent
    data class TypeChanged(val type: String) : ChallengeIntent
    data class RequireAllMembersCompletionChanged(val requireAll: Boolean) : ChallengeIntent
    data class SubmitCreateChallenge(val teamId: String, val creatorId: String) : ChallengeIntent
    data class JoinChallenge(val challengeId: String, val userId: String) : ChallengeIntent
    data object ClearMessages : ChallengeIntent
}
