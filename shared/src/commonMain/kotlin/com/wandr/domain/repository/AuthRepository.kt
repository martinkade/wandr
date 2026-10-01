package com.wandr.domain.repository

import com.wandr.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun currentSession(): Flow<AuthSession?>
    /** Suspends until the stored session has been restored, then reports whether one is present. */
    suspend fun hasActiveSession(): Boolean
    suspend fun login(email: String, password: String): Result<AuthSession>
    suspend fun register(email: String, password: String, username: String): Result<AuthSession>
    suspend fun logout(): Result<Unit>
}
