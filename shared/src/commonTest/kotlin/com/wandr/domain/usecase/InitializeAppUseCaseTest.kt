package com.wandr.domain.usecase

import com.wandr.domain.error.AppError
import com.wandr.domain.model.AuthSession
import com.wandr.domain.repository.AppStorageRepository
import com.wandr.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeStorage(val failure: Throwable? = null) : AppStorageRepository {
    override suspend fun prepare() {
        failure?.let { throw it }
    }
}

private class FakeAuth(val hasSession: Boolean, val failure: Throwable? = null) : AuthRepository {
    override fun currentSession(): Flow<AuthSession?> = flowOf(null)
    override suspend fun hasActiveSession(): Boolean = failure?.let { throw it } ?: hasSession
    override suspend fun login(email: String, password: String) =
        Result.failure<AuthSession>(UnsupportedOperationException())

    override suspend fun register(email: String, password: String, username: String) =
        Result.failure<AuthSession>(UnsupportedOperationException())

    override suspend fun logout() = Result.success(Unit)
}

class InitializeAppUseCaseTest {
    @Test
    fun reportsWhetherASessionIsPresent() = runTest {
        assertTrue(InitializeAppUseCase(FakeStorage(), FakeAuth(true))().getOrThrow())
        assertEquals(false, InitializeAppUseCase(FakeStorage(), FakeAuth(false))().getOrThrow())
    }

    @Test
    fun aStorageProblemIsReportedAsLocalStorageWithItsCause() = runTest {
        val cause = IllegalStateException("database is locked")
        val error = InitializeAppUseCase(FakeStorage(cause), FakeAuth(true))().exceptionOrNull()
        assertIs<AppError.LocalStorage>(error)
        assertEquals(cause, error.cause)
    }

    @Test
    fun otherFailuresBecomeAppErrors() = runTest {
        val error = InitializeAppUseCase(
            FakeStorage(),
            FakeAuth(true, failure = IllegalStateException("boom"))
        )().exceptionOrNull()
        assertIs<AppError.Unknown>(error)
    }
}
