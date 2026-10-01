package com.wandr.presentation.auth

sealed interface LoginIntent {
    data class EmailChanged(val email: String) : LoginIntent
    data class PasswordChanged(val password: String) : LoginIntent
    data object SubmitLogin : LoginIntent
    data object SubmitRegister : LoginIntent
    data object ClearError : LoginIntent
}
