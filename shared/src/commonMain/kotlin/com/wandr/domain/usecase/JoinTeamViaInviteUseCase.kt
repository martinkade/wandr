package com.wandr.domain.usecase

import com.wandr.domain.model.Team
import com.wandr.domain.repository.TeamRepository

class JoinTeamViaInviteUseCase(private val teamRepository: TeamRepository) {
    suspend operator fun invoke(inviteCode: String, userId: String): Result<Team> {
        if (inviteCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Invite code cannot be empty"))
        }
        return teamRepository.joinTeamViaInvite(inviteCode.trim(), userId)
    }
}
