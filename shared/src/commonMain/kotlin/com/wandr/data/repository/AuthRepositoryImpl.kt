package com.wandr.data.repository

import com.wandr.data.remote.supabaseResult
import com.wandr.domain.error.AppError
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

    override suspend fun login(email: String, password: String): Result<AuthSession> =
        supabaseResult {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
            val session = supabase.auth.currentSessionOrNull() ?: throw AppError.Unknown(
                IllegalStateException("Signed in, but there is no session")
            )
            val user = supabase.auth.currentUserOrNull() ?: throw AppError.Unknown(
                IllegalStateException("Signed in, but there is no user")
            )
        AuthSession(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            user = User(id = user.id, email = user.email ?: "")
        )
    }

    override suspend fun register(
        email: String,
        password: String,
        username: String
    ): Result<AuthSession> = supabaseResult {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("username", username)
                put("display_name", username)
            }
        }
        // With "confirm email" enabled the sign-up creates the account but no session yet.
        val session =
            supabase.auth.currentSessionOrNull() ?: throw AppError.EmailConfirmationRequired()
        val user = supabase.auth.currentUserOrNull()
            ?: throw AppError.Unknown(IllegalStateException("Signed up, but there is no user"))
        AuthSession(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            user = User(id = user.id, email = user.email ?: "")
        )
    }

    override suspend fun logout(): Result<Unit> = supabaseResult {
        supabase.auth.signOut()
    }
}
