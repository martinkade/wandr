package com.wandr.data.repository

import com.wandr.domain.model.AuthSession
import com.wandr.domain.model.User
import com.wandr.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepositoryImpl(
    private val supabase: SupabaseClient
) : AuthRepository {

    override fun currentSession(): Flow<AuthSession?> {
        return supabase.auth.sessionStatus.map { status ->
            val session = supabase.auth.currentSessionOrNull()
            val user = supabase.auth.currentUserOrNull()
            if (session != null && user != null) {
                AuthSession(
                    accessToken = session.accessToken,
                    refreshToken = session.refreshToken,
                    user = User(id = user.id, email = user.email ?: "")
                )
            } else null
        }
    }

    override suspend fun hasActiveSession(): Boolean {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentSessionOrNull() != null
    }

    override suspend fun login(email: String, password: String): Result<AuthSession> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val session = supabase.auth.currentSessionOrNull() ?: throw IllegalStateException("No active session")
        val user = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("No authenticated user")
        AuthSession(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            user = User(id = user.id, email = user.email ?: "")
        )
    }

    override suspend fun register(email: String, password: String, username: String): Result<AuthSession> = runCatching {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("username", username)
                put("display_name", username)
            }
        }
        val session = supabase.auth.currentSessionOrNull() ?: throw IllegalStateException("Registration successful. Please verify email or log in.")
        val user = supabase.auth.currentUserOrNull() ?: throw IllegalStateException("No authenticated user")
        AuthSession(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            user = User(id = user.id, email = user.email ?: "")
        )
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        supabase.auth.signOut()
    }
}
