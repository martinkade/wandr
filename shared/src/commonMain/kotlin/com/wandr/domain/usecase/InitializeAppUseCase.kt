package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.error.asAppError
import com.wandr.domain.repository.AppStorageRepository
import com.wandr.domain.repository.AuthRepository
import kotlin.coroutines.cancellation.CancellationException

/**
 * App start-up work: prepare local storage (database migrations), then evaluate the stored session.
 * Returns `true` when an authenticated session is present. A storage problem is reported as [AppError.LocalStorage].
 */
class InitializeAppUseCase(
    private val appStorageRepository: AppStorageRepository,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Boolean> = try {
        try {
            appStorageRepository.prepare()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            throw AppError.LocalStorage(error)
        }
        Result.success(authRepository.hasActiveSession())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        Result.failure(error.asAppError())
    }
}
