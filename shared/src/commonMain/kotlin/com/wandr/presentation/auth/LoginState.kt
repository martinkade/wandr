package com.wandr.presentation.auth

import com.wandr.domain.error.AppError

data class LoginState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    /** What went wrong, or null; the UI turns the type into a localized text (see `AppError`). */
    val error: AppError? = null,
    val isAuthenticated: Boolean = false,
    /** The account was created but the email address must be confirmed first: the screen shows a sheet that leads to sign-in. */
    val needsEmailConfirmation: Boolean = false
)
