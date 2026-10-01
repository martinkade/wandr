package com.wandr.domain.usecase

import com.wandr.domain.model.Profile
import com.wandr.domain.repository.ProfileRepository

class UploadAvatarUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(userId: String, jpegBytes: ByteArray): Result<Profile> {
        if (jpegBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Avatar image file is empty"))
        }
        if (jpegBytes.size > MAX_AVATAR_BYTES) {
            return Result.failure(IllegalArgumentException("Avatar image is too large"))
        }
        return profileRepository.setAvatar(userId, jpegBytes)
    }

    companion object {
        const val MAX_AVATAR_BYTES = 2 * 1024 * 1024
    }
}
