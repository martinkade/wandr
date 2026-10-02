package com.wandr.android.ui.challenge

import com.wandr.domain.model.ChallengeAction
import com.wandr.domain.model.ChallengeParticipation
import com.wandr.presentation.challenge.ChallengeIntent

/** Turns the offered [ChallengeAction] into the matching intent; enrolling first asks which team via [onChooseTeam]. */
internal fun performChallengeAction(
    action: ChallengeAction,
    challengeId: String,
    participation: ChallengeParticipation?,
    userId: String,
    onIntent: (ChallengeIntent) -> Unit,
    onChooseTeam: () -> Unit
) {
    when (action) {
        ChallengeAction.JOIN -> onIntent(ChallengeIntent.JoinChallenge(challengeId, userId))
        ChallengeAction.LEAVE -> onIntent(ChallengeIntent.LeaveChallenge(challengeId, userId))
        ChallengeAction.ENROLL_TEAM -> onChooseTeam()
        ChallengeAction.WITHDRAW_TEAM -> participation?.teamId?.let { onIntent(ChallengeIntent.WithdrawTeam(challengeId, it)) }
    }
}
