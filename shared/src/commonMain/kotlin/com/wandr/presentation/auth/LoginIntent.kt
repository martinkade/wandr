package com.wandr.presentation.auth

sealed interface LoginIntent {
    data class EmailChanged(val email: String) : LoginIntent
    data class PasswordChanged(val password: String) : LoginIntent
    data object SubmitLogin : LoginIntent
    data object SubmitRegister : LoginIntent

    /** The user read the "confirm your email" sheet and goes on to the sign-in screen. */
    data object ContinueToLogin : LoginIntent
    data object ClearError : LoginIntent
}
