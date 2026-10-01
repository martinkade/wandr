package com.wandr.domain.usecase

import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamImageKind
import com.wandr.domain.repository.TeamRepository

class SetTeamImageUseCase(private val teamRepository: TeamRepository) {
    suspend operator fun invoke(teamId: String, kind: TeamImageKind, jpegBytes: ByteArray): Result<Team> {
        if (jpegBytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("Image file is empty"))
        }
        if (jpegBytes.size > MAX_IMAGE_BYTES) {
            return Result.failure(IllegalArgumentException("Image is too large"))
        }
        return teamRepository.setTeamImage(teamId, kind, jpegBytes)
    }

    companion object {
        const val MAX_IMAGE_BYTES = 4 * 1024 * 1024
    }
}
