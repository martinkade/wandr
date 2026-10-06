package com.wandr.domain.usecase

import com.wandr.domain.model.SocialCounts
import com.wandr.domain.model.SocialEntityType
import com.wandr.domain.repository.SocialRepository

/** Likes and comments of a whole list (e.g. the feed) in one call. */
class GetSocialCountsUseCase(private val repository: SocialRepository) {
    suspend operator fun invoke(type: SocialEntityType, entityIds: List<String>): Result<Map<String, SocialCounts>> =
        if (entityIds.isEmpty()) Result.success(emptyMap()) else repository.getCounts(type, entityIds)
}
