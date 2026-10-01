package com.wandr.domain.usecase

import com.wandr.domain.model.AuthSession
import com.wandr.domain.repository.AuthRepository

class RegisterUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, username: String): Result<AuthSession> {
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }
        if (username.isBlank()) {
            return Result.failure(IllegalArgumentException("Username cannot be blank"))
        }
        return authRepository.register(email.trim(), password, username.trim())
    }
}
