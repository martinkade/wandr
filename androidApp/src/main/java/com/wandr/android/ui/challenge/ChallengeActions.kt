package com.wandr.android.ui.challenge

import com.wandr.domain.model.ChallengeAction
import com.wandr.presentation.challenge.ChallengeIntent

/**
 * Turns the offered [ChallengeAction] into the matching intent. Enrolling first asks which team via [onChooseTeam];
 * withdrawing needs the administered team that is enrolled ([withdrawTeamId]).
 */
internal fun performChallengeAction(
    action: ChallengeAction,
    challengeId: String,
    withdrawTeamId: String?,
    userId: String,
    onIntent: (ChallengeIntent) -> Unit,
    onChooseTeam: () -> Unit
) {
    when (action) {
        ChallengeAction.JOIN -> onIntent(ChallengeIntent.JoinChallenge(challengeId, userId))
        ChallengeAction.LEAVE -> onIntent(ChallengeIntent.LeaveChallenge(challengeId, userId))
        ChallengeAction.ENROLL_TEAM -> onChooseTeam()
        ChallengeAction.WITHDRAW_TEAM -> withdrawTeamId?.let {
            onIntent(
                ChallengeIntent.WithdrawTeam(
                    challengeId,
                    it
                )
            )
        }
    }
}
