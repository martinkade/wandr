package com.wandr.domain.usecase

import com.wandr.domain.repository.ProfileRepository

class UploadAvatarUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(userId: String, bytes: ByteArray, fileName: String): Result<String> {
        if (bytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Avatar image file is empty"))
        }
        return profileRepository.uploadAvatar(userId, bytes, fileName)
    }
}
