package com.wandr.domain.model

/** The one participation action a challenge offers the user right now (see [availableChallengeAction]). */
enum class ChallengeAction { JOIN, LEAVE, ENROLL_TEAM, WITHDRAW_TEAM }

/**
 * Which action to offer for [challenge], given its runtime [status]:
 *
 * - Individual challenges: join (while planned / active), leave (until it is over) -- [participation] decides.
 * - Group challenges are about TEAMS, and only the team's owner / admins may enroll or withdraw it (the server enforces
 *   the same). [adminTeamIds] are the teams the user administers, [enrolledTeamIds] the teams already enrolled in this
 *   challenge. A default member sees no button at all. Withdrawing is offered until the challenge is over, enrolling
 *   only while it is open (planned or active, never a draft) and if the user administers a team that is not enrolled yet.
 */
fun availableChallengeAction(
    challenge: Challenge,
    status: ChallengeStatus,
    participation: ChallengeParticipation?,
    adminTeamIds: Set<String>,
    enrolledTeamIds: Set<String>
): ChallengeAction? {
    val isOver = status == ChallengeStatus.COMPLETED || status == ChallengeStatus.EXPIRED
    val isOpen = status == ChallengeStatus.PLANNED || status == ChallengeStatus.ACTIVE
    val isGroup = ChallengeScope.fromValue(challenge.scope) == ChallengeScope.GROUP
    return when {
        isGroup -> when {
            isOver -> null
            adminTeamIds.any { it in enrolledTeamIds } -> ChallengeAction.WITHDRAW_TEAM
            isOpen && adminTeamIds.any { it !in enrolledTeamIds } -> ChallengeAction.ENROLL_TEAM
            else -> null
        }

        participation != null -> ChallengeAction.LEAVE.takeIf { !isOver }
        isOpen -> ChallengeAction.JOIN
        else -> null
    }
}
