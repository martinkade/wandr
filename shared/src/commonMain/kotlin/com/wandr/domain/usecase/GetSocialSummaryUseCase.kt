package com.wandr.domain.usecase

import com.wandr.domain.model.SocialEntityType
import com.wandr.domain.repository.SocialRepository

class GetSocialSummaryUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(type: SocialEntityType, entityId: String, userId: String) = repository.getSummary(type, entityId, userId)
}
