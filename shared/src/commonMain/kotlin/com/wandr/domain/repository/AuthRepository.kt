package com.wandr.domain.repository

import com.wandr.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun currentSession(): Flow<AuthSession?>
    suspend fun login(email: String, password: String): Result<AuthSession>
    suspend fun register(email: String, password: String, username: String): Result<AuthSession>
    suspend fun logout(): Result<Unit>
}
