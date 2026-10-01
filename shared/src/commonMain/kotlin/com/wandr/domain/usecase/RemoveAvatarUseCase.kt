package com.wandr.domain.usecase

import com.wandr.domain.model.Profile
import com.wandr.domain.repository.ProfileRepository

class RemoveAvatarUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(userId: String): Result<Profile> = profileRepository.removeAvatar(userId)
}
