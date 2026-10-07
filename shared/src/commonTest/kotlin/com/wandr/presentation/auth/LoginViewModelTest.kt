package com.wandr.presentation.auth

import com.wandr.domain.error.AppError
import com.wandr.domain.model.AuthSession
import com.wandr.domain.model.User
import com.wandr.domain.repository.AuthRepository
import com.wandr.domain.usecase.LoginUseCase
import com.wandr.domain.usecase.RegisterUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAuthRepository(var failure: Throwable? = null) : AuthRepository {
    var loginCalls = 0
    var registerCalls = 0
    private val session = AuthSession("access", "refresh", User("u1", "a@b.de"))

    override fun currentSession(): Flow<AuthSession?> = flowOf(null)
    override suspend fun hasActiveSession() = false
    override suspend fun login(email: String, password: String): Result<AuthSession> {
        loginCalls++
        return failure?.let { Result.failure(it) } ?: Result.success(session)
    }

    override suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<AuthSession> {
        registerCalls++
        return failure?.let { Result.failure(it) } ?: Result.success(session)
    }

    override suspend fun logout() = Result.success(Unit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private fun kotlinx.coroutines.test.TestScope.vm(repo: FakeAuthRepository) = LoginViewModel(
        LoginUseCase(repo),
        RegisterUseCase(repo),
        CoroutineScope(UnconfinedTestDispatcher(testScheduler))
    )

    private fun LoginViewModel.enter(email: String, password: String) {
        processIntent(LoginIntent.EmailChanged(email))
        processIntent(LoginIntent.PasswordChanged(password))
    }

    @Test
    fun wrongCredentialsShowATypedErrorNotTheServerText() = runTest {
        val repo = FakeAuthRepository(failure = AppError.InvalidCredentials())
        val vm = vm(repo)
        vm.enter("a@b.de", "secret1")
        vm.processIntent(LoginIntent.SubmitLogin)

        assertIs<AppError.InvalidCredentials>(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isAuthenticated)
    }

    @Test
    fun invalidInputIsRejectedBeforeAnyRequest() = runTest {
        val repo = FakeAuthRepository()
        val vm = vm(repo)

        vm.enter("not-an-email", "secret1")
        vm.processIntent(LoginIntent.SubmitLogin)
        assertIs<AppError.InvalidEmail>(vm.uiState.value.error)

        vm.enter("a@b.de", "123")
        vm.processIntent(LoginIntent.SubmitLogin)
        assertEquals(6, assertIs<AppError.PasswordTooShort>(vm.uiState.value.error).minLength)

        assertEquals(0, repo.loginCalls)
    }

    @Test
    fun registrationChecksTheUsernameAndMapsServerErrors() = runTest {
        val repo = FakeAuthRepository(failure = AppError.UserAlreadyExists())
        val vm = vm(repo)
        vm.enter("a@b.de", "secret1")
        vm.processIntent(LoginIntent.SubmitRegister)
        assertIs<AppError.UserAlreadyExists>(vm.uiState.value.error)

        val blank = vm(FakeAuthRepository())
        blank.enter("@b.de", "secret1") // the username is the part before the @
        blank.processIntent(LoginIntent.SubmitRegister)
        assertIs<AppError.UsernameRequired>(blank.uiState.value.error)
    }

    @Test
    fun registrationWithEmailConfirmationShowsTheConfirmSheetAndNoError() = runTest {
        val vm = vm(FakeAuthRepository(failure = AppError.EmailConfirmationRequired()))
        vm.enter("a@b.de", "secret1")
        vm.processIntent(LoginIntent.SubmitRegister)

        assertTrue(vm.uiState.value.needsEmailConfirmation)
        assertNull(vm.uiState.value.error) // good news, not an error
        assertFalse(vm.uiState.value.isAuthenticated)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun registrationThatTheServerSignsInGoesStraightIntoTheAppWithoutASheet() = runTest {
        val vm = vm(FakeAuthRepository())
        vm.enter("a@b.de", "secret1")
        vm.processIntent(LoginIntent.SubmitRegister)

        assertTrue(vm.uiState.value.isAuthenticated)
        assertFalse(vm.uiState.value.needsEmailConfirmation)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun continuingToTheSignInScreenClosesTheSheetAndForgetsThePassword() = runTest {
        val vm = vm(FakeAuthRepository(failure = AppError.EmailConfirmationRequired()))
        vm.enter("a@b.de", "secret1")
        vm.processIntent(LoginIntent.SubmitRegister)
        vm.processIntent(LoginIntent.ContinueToLogin)

        assertFalse(vm.uiState.value.needsEmailConfirmation)
        assertEquals("", vm.uiState.value.passwordInput)
        assertEquals("a@b.de", vm.uiState.value.emailInput) // handy for the sign-in
    }

    @Test
    fun aFailedRegistrationShowsNoSheet() = runTest {
        val vm = vm(FakeAuthRepository(failure = AppError.UserAlreadyExists()))
        vm.enter("a@b.de", "secret1")
        vm.processIntent(LoginIntent.SubmitRegister)
        assertFalse(vm.uiState.value.needsEmailConfirmation)
        assertIs<AppError.UserAlreadyExists>(vm.uiState.value.error)
    }

    @Test
    fun anythingElseThatFailsBecomesAnUnknownError() = runTest {
        val vm = vm(FakeAuthRepository(failure = IllegalStateException("boom")))
        vm.enter("a@b.de", "secret1")
        vm.processIntent(LoginIntent.SubmitLogin)
        assertIs<AppError.Unknown>(vm.uiState.value.error)
    }

    @Test
    fun theErrorClearsWhenTheUserTypesAgainAndOnSuccess() = runTest {
        val repo = FakeAuthRepository(failure = AppError.Network())
        val vm = vm(repo)
        vm.enter("a@b.de", "secret1")
        vm.processIntent(LoginIntent.SubmitLogin)
        assertIs<AppError.Network>(vm.uiState.value.error)

        vm.processIntent(LoginIntent.PasswordChanged("secret12"))
        assertNull(vm.uiState.value.error)

        repo.failure = null
        vm.processIntent(LoginIntent.SubmitLogin)
        assertTrue(vm.uiState.value.isAuthenticated)
        assertNull(vm.uiState.value.error)
    }
}
