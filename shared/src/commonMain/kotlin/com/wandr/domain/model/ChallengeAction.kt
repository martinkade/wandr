package com.wandr.domain.model

/** The one participation action a challenge offers the user right now (see [availableChallengeAction]). */
enum class ChallengeAction { JOIN, LEAVE, ENROLL_TEAM, WITHDRAW_TEAM }

/**
 * Which action to offer for [challenge], given its runtime [status] and the user's [participation]:
 *
 * - Joining / enrolling is only possible while the challenge is open: planned or active, never a draft and never
 *   over (completed / expired).
 * - Leaving / withdrawing is offered as long as the challenge is not over.
 * - Individual challenges: join or leave. Group challenges: enroll one of the user's teams ([hasTeams]) or
 *   withdraw the enrolled team.
 */
fun availableChallengeAction(
    challenge: Challenge,
    status: ChallengeStatus,
    participation: ChallengeParticipation?,
    hasTeams: Boolean
): ChallengeAction? {
    val isOver = status == ChallengeStatus.COMPLETED || status == ChallengeStatus.EXPIRED
    val isOpen = status == ChallengeStatus.PLANNED || status == ChallengeStatus.ACTIVE
    val isGroup = ChallengeScope.fromValue(challenge.scope) == ChallengeScope.GROUP
    return when {
        participation != null -> when {
            isOver -> null
            isGroup -> ChallengeAction.WITHDRAW_TEAM
            else -> ChallengeAction.LEAVE
        }
        !isOpen -> null
        isGroup -> ChallengeAction.ENROLL_TEAM.takeIf { hasTeams }
        else -> ChallengeAction.JOIN
    }
}
