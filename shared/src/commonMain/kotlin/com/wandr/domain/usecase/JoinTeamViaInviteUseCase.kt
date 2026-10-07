package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem
import com.wandr.domain.model.InviteCode
import com.wandr.domain.model.Team
import com.wandr.domain.repository.TeamRepository

class JoinTeamViaInviteUseCase(private val teamRepository: TeamRepository) {
    suspend operator fun invoke(inviteCode: String, userId: String): Result<Team> {
        if (inviteCode.isBlank()) {
            return Result.failure(AppError.InvalidInput(InputProblem.INVITE_CODE_REQUIRED))
        }
        // Accepts the bare code as well as the content of an invite QR code (wandr://invite/{code}).
        val code = InviteCode.parse(inviteCode) ?: return Result.failure(
            AppError.InvalidInput(
                InputProblem.INVITE_CODE_INVALID
            )
        )
        return teamRepository.joinTeamViaInvite(code, userId)
    }
}
