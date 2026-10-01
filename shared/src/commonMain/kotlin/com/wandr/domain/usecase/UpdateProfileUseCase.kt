package com.wandr.domain.usecase

import com.wandr.domain.model.Profile
import com.wandr.domain.repository.ProfileRepository

class UpdateProfileUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(profile: Profile): Result<Profile> {
        if (profile.displayName.isBlank()) {
            return Result.failure(IllegalArgumentException("Display name cannot be empty"))
        }
        return profileRepository.updateProfile(profile)
    }
}
