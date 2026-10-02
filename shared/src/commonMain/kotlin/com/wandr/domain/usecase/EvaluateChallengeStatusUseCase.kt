package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeStatus

/**
 * Derives the [ChallengeStatus] shown to the user at runtime. Only draft / active is stored on the server, the rest
 * follows from [Challenge.isActive], the dates and the participants' progress:
 *
 * - not published: DRAFT
 * - before the start date: PLANNED
 * - completion reached (all participants with `requireAllMembersCompletion`, otherwise at least one): COMPLETED
 * - after the end date without completion: EXPIRED
 * - otherwise: ACTIVE
 *
 * Without participant data (counts 0) the result only depends on the flag and the dates.
 */
class EvaluateChallengeStatusUseCase {
    operator fun invoke(
        challenge: Challenge,
        totalParticipantsCount: Int = 0,
        completedParticipantsCount: Int = 0,
        currentTimeMillis: Long
    ): ChallengeStatus {
        if (!challenge.isActive) return ChallengeStatus.DRAFT
        if (currentTimeMillis < challenge.startDate) return ChallengeStatus.PLANNED

        val isCompleted = if (challenge.requireAllMembersCompletion) {
            totalParticipantsCount > 0 && completedParticipantsCount == totalParticipantsCount
        } else {
            completedParticipantsCount > 0
        }
        return when {
            isCompleted -> ChallengeStatus.COMPLETED
            currentTimeMillis > challenge.endDate -> ChallengeStatus.EXPIRED
            else -> ChallengeStatus.ACTIVE
        }
    }
}
