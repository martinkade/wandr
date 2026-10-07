package com.wandr.presentation.auth

import com.wandr.domain.error.asAppError
import com.wandr.domain.usecase.LoginUseCase
import com.wandr.domain.usecase.RegisterUseCase
import com.wandr.domain.usecase.RegistrationOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _uiState = MutableStateFlow(LoginState())
    val uiState: StateFlow<LoginState> = _uiState.asStateFlow()

    fun processIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> {
                _uiState.update { it.copy(emailInput = intent.email, error = null) }
            }
            is LoginIntent.PasswordChanged -> {
                _uiState.update { it.copy(passwordInput = intent.password, error = null) }
            }
            is LoginIntent.SubmitLogin -> performLogin()
            is LoginIntent.SubmitRegister -> performRegister()
            is LoginIntent.ContinueToLogin -> _uiState.update {
                it.copy(needsEmailConfirmation = false, passwordInput = "")
            }

            is LoginIntent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun performLogin() {
        val email = _uiState.value.emailInput
        val password = _uiState.value.passwordInput
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        scope.launch {
            val result = loginUseCase(email, password)
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.asAppError()) }
            }
        }
    }

    private fun performRegister() {
        val email = _uiState.value.emailInput
        val password = _uiState.value.passwordInput
        val username = email.substringBefore("@")
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        scope.launch {
            val result = registerUseCase(email, password, username)
            result.onSuccess { outcome ->
                _uiState.update {
                    when (outcome) {
                        // The server signed the user in: straight into the app.
                        RegistrationOutcome.SIGNED_IN -> it.copy(
                            isLoading = false,
                            isAuthenticated = true
                        )

                        RegistrationOutcome.EMAIL_CONFIRMATION_REQUIRED -> it.copy(
                            isLoading = false,
                            needsEmailConfirmation = true
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.asAppError()) }
            }
        }
    }
}
