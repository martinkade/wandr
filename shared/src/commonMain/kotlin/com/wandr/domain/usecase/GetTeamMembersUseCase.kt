package com.wandr.domain.usecase

import com.wandr.domain.model.TeamMember
import com.wandr.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow

/** Members of a team with their profile data, admins first. */
class GetTeamMembersUseCase(private val teamRepository: TeamRepository) {
    operator fun invoke(teamId: String): Flow<List<TeamMember>> = teamRepository.getTeamMembers(teamId)
}
