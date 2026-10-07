package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.model.AuthSession
import com.wandr.domain.repository.AuthRepository

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<AuthSession> {
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(AppError.InvalidEmail())
        }
        if (password.length < AppError.PasswordTooShort.MIN_PASSWORD_LENGTH) {
            return Result.failure(AppError.PasswordTooShort())
        }
        return authRepository.login(email.trim(), password)
    }
}
