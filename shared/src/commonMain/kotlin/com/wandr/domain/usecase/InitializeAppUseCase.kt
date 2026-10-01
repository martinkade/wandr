package com.wandr.domain.usecase

import com.wandr.domain.repository.AppStorageRepository
import com.wandr.domain.repository.AuthRepository

/**
 * App start-up work: prepare local storage (database migrations), then evaluate the stored session.
 * Returns `true` when an authenticated session is present.
 */
class InitializeAppUseCase(
    private val appStorageRepository: AppStorageRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Boolean> = runCatching {
        appStorageRepository.prepare()
        authRepository.hasActiveSession()
    }
}
