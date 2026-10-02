package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeStatus

/**
 * Derives the current [ChallengeStatus] at runtime; nothing but draft / active is stored on the server.
 *
 * - before the start date: DRAFT
 * - completion reached (all participants with `requireAllMembersCompletion`, otherwise at least one): COMPLETED
 * - after the end date without completion: EXPIRED
 * - otherwise: ACTIVE
 *
 * Without participant data (counts 0) the result only depends on the dates: DRAFT, ACTIVE or EXPIRED.
 */
class EvaluateChallengeStatusUseCase {
    operator fun invoke(
        challenge: Challenge,
        totalParticipantsCount: Int = 0,
        completedParticipantsCount: Int = 0,
        currentTimeMillis: Long
    ): ChallengeStatus {
        if (currentTimeMillis < challenge.startDate) return ChallengeStatus.DRAFT

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
