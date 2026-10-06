package com.wandr.domain.usecase

import com.wandr.domain.model.Profile
import com.wandr.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow

/** The cached profiles of several users, e.g. the authors of the activities in the feed. */
class GetProfilesUseCase(private val repository: ProfileRepository) {
    operator fun invoke(userIds: List<String>): Flow<List<Profile>> = repository.getProfiles(userIds)
}
