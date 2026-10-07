package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem
import com.wandr.domain.model.Challenge
import com.wandr.domain.repository.ChallengeRepository

class SetChallengeCoverUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(challengeId: String, jpegBytes: ByteArray): Result<Challenge> {
        if (jpegBytes.isEmpty()) return Result.failure(AppError.InvalidInput(InputProblem.IMAGE_EMPTY))
        if (jpegBytes.size > MAX_COVER_BYTES) return Result.failure(
            AppError.InvalidInput(
                InputProblem.IMAGE_TOO_LARGE,
                MAX_COVER_BYTES
            )
        )
        return challengeRepository.setChallengeCover(challengeId, jpegBytes)
    }

    companion object {
        const val MAX_COVER_BYTES = 4 * 1024 * 1024
    }
}
