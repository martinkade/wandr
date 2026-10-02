package com.wandr.presentation.challenge

import com.wandr.domain.model.ChallengeScope
import com.wandr.domain.model.ChallengeType

sealed interface ChallengeIntent {
    data class LoadChallenges(val userId: String) : ChallengeIntent
    /** Loads the team standings; with [teamId] (the user's team) also that team's member progress. */
    data class SelectChallenge(val challengeId: String, val teamId: String? = null) : ChallengeIntent

    /** Opens an empty form (create). Managers only; the server enforces it too. */
    data object StartCreate : ChallengeIntent

    /** Opens the form filled with an existing challenge (edit). */
    data class StartEdit(val challengeId: String) : ChallengeIntent
    data class TitleChanged(val title: String) : ChallengeIntent
    data class DescriptionChanged(val description: String) : ChallengeIntent
    data class TypeChanged(val type: ChallengeType) : ChallengeIntent

    /** In base units (meters / seconds); see [ChallengeUnits]. */
    data class TargetValueChanged(val value: Double) : ChallengeIntent
    /** Epoch milliseconds. If the new start is not before the end, the end moves along (same length, at least a day). */
    data class StartDateChanged(val millis: Long) : ChallengeIntent
    data class EndDateChanged(val millis: Long) : ChallengeIntent
    /** Status toggle: active (published) or draft. */
    data class ActiveChanged(val isActive: Boolean) : ChallengeIntent
    data class ScopeChanged(val scope: ChallengeScope) : ChallengeIntent
    data class RequireAllMembersCompletionChanged(val requireAll: Boolean) : ChallengeIntent

    /** Creates or updates, depending on the form. */
    data class SubmitForm(val userId: String) : ChallengeIntent

    /** Closes the form and drops its content. */
    data object DiscardForm : ChallengeIntent

    /** [jpegBytes] is the already cropped and scaled cover (see CoverImageSpec); applies to the selected challenge. */
    data class UploadCover(val jpegBytes: ByteArray) : ChallengeIntent
    data object RemoveCover : ChallengeIntent
    /** Individual challenges only. */
    data class JoinChallenge(val challengeId: String, val userId: String) : ChallengeIntent

    /** Individual challenges: leaves a challenge the user has joined. */
    data class LeaveChallenge(val challengeId: String, val userId: String) : ChallengeIntent

    /** Group challenges: withdraws the enrolled team (owner / admin only, enforced by the server). */
    data class WithdrawTeam(val challengeId: String, val teamId: String) : ChallengeIntent

    /** Group challenges: enrolls one of the user's teams (owner / admin only, enforced by the server). */
    data class EnrollTeam(val challengeId: String, val teamId: String, val userId: String) : ChallengeIntent
    data object ClearMessages : ChallengeIntent
}
