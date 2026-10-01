package com.wandr.domain.usecase

import com.wandr.domain.model.AuthSession
import com.wandr.domain.repository.AuthRepository

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<AuthSession> {
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }
        return authRepository.login(email.trim(), password)
    }
}
