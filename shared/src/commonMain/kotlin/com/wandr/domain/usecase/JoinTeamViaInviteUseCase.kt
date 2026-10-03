package com.wandr.domain.usecase

import com.wandr.domain.model.InviteCode
import com.wandr.domain.model.Team
import com.wandr.domain.repository.TeamRepository

class JoinTeamViaInviteUseCase(private val teamRepository: TeamRepository) {
    suspend operator fun invoke(inviteCode: String, userId: String): Result<Team> {
        if (inviteCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Invite code cannot be empty"))
        }
        // Accepts the bare code as well as the content of an invite QR code (wandr://invite/{code}).
        val code = InviteCode.parse(inviteCode) ?: return Result.failure(IllegalArgumentException("Invalid invite code"))
        return teamRepository.joinTeamViaInvite(code, userId)
    }
}
