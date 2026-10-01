package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge

class EvaluateChallengeStatusUseCase {
    operator fun invoke(
        challenge: Challenge,
        totalParticipantsCount: Int,
        completedParticipantsCount: Int,
        currentTimeMillis: Long
    ): String {
        val isTimeExpired = currentTimeMillis > challenge.endDate
        val isBeforeStart = currentTimeMillis < challenge.startDate

        if (isBeforeStart) {
            return "planned"
        }

        if (challenge.requireAllMembersCompletion) {
            val allCompleted = totalParticipantsCount > 0 && completedParticipantsCount == totalParticipantsCount
            return when {
                allCompleted -> "completed"
                isTimeExpired -> "expired"
                else -> "active"
            }
        } else {
            return when {
                completedParticipantsCount > 0 -> "completed"
                isTimeExpired -> "expired"
                else -> "active"
            }
        }
    }
}
