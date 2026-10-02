package com.wandr.domain.usecase

import com.wandr.domain.model.Challenge
import com.wandr.domain.repository.ChallengeRepository

class SetChallengeCoverUseCase(private val challengeRepository: ChallengeRepository) {
    suspend operator fun invoke(challengeId: String, jpegBytes: ByteArray): Result<Challenge> {
        if (jpegBytes.isEmpty()) return Result.failure(IllegalArgumentException("Image file is empty"))
        if (jpegBytes.size > MAX_COVER_BYTES) return Result.failure(IllegalArgumentException("Image is too large"))
        return challengeRepository.setChallengeCover(challengeId, jpegBytes)
    }

    companion object {
        const val MAX_COVER_BYTES = 4 * 1024 * 1024
    }
}
