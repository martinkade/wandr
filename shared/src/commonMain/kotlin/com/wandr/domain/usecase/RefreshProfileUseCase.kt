package com.wandr.domain.usecase

import com.wandr.domain.repository.ProfileRepository

class RefreshProfileUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(userId: String): Result<Unit> = profileRepository.refreshProfile(userId)
}
