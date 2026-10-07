package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem
import com.wandr.domain.model.Profile
import com.wandr.domain.repository.ProfileRepository

class UpdateProfileUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(profile: Profile): Result<Profile> {
        if (profile.displayName.isBlank()) {
            return Result.failure(AppError.InvalidInput(InputProblem.DISPLAY_NAME_REQUIRED))
        }
        return profileRepository.updateProfile(profile)
    }
}
