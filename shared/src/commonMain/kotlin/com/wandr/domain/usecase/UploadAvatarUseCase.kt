package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem
import com.wandr.domain.model.Profile
import com.wandr.domain.repository.ProfileRepository

class UploadAvatarUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(userId: String, jpegBytes: ByteArray): Result<Profile> {
        if (jpegBytes.isEmpty()) {
            return Result.failure(AppError.InvalidInput(InputProblem.IMAGE_EMPTY))
        }
        if (jpegBytes.size > MAX_AVATAR_BYTES) {
            return Result.failure(
                AppError.InvalidInput(
                    InputProblem.IMAGE_TOO_LARGE,
                    MAX_AVATAR_BYTES
                )
            )
        }
        return profileRepository.setAvatar(userId, jpegBytes)
    }

    companion object {
        const val MAX_AVATAR_BYTES = 2 * 1024 * 1024
    }
}
