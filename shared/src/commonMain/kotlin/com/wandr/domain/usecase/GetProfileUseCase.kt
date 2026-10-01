package com.wandr.domain.usecase

import com.wandr.domain.model.Profile
import com.wandr.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow

class GetProfileUseCase(private val profileRepository: ProfileRepository) {
    operator fun invoke(userId: String): Flow<Profile?> {
        return profileRepository.getProfile(userId)
    }
}
