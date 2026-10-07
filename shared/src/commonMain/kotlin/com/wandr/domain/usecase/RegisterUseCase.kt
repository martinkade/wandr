package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.repository.AuthRepository

/** How a successful sign-up ended. */
enum class RegistrationOutcome {
    /** The account exists and the server signed the new user in already. */
    SIGNED_IN,

    /** The account exists, but the email address has to be confirmed (link in an email) before signing in. */
    EMAIL_CONFIRMATION_REQUIRED
}

class RegisterUseCase(private val authRepository: AuthRepository) {
    /**
     * Creates the account. Either outcome is a success: the server signs the new user in right away
     * ([RegistrationOutcome.SIGNED_IN]), or the email address has to be confirmed first.
     */
    suspend operator fun invoke(
        email: String,
        password: String,
        username: String
    ): Result<RegistrationOutcome> {
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(AppError.InvalidEmail())
        }
        if (password.length < AppError.PasswordTooShort.MIN_PASSWORD_LENGTH) {
            return Result.failure(AppError.PasswordTooShort())
        }
        if (username.isBlank()) {
            return Result.failure(AppError.UsernameRequired())
        }
        return authRepository.register(email.trim(), password, username.trim()).fold(
            onSuccess = { Result.success(RegistrationOutcome.SIGNED_IN) },
            onFailure = { error ->
                if (error is AppError.EmailConfirmationRequired) Result.success(RegistrationOutcome.EMAIL_CONFIRMATION_REQUIRED)
                else Result.failure(error)
            }
        )
    }
}
