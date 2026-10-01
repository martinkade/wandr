package com.wandr.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.plugins.auth.providers.BearerTokens

class SupabaseTokenProvider(
    private val supabase: SupabaseClient
) : TokenProvider {

    override suspend fun getAccessToken(): String? {
        return supabase.auth.currentSessionOrNull()?.accessToken
    }

    override suspend fun getRefreshToken(): String? {
        return supabase.auth.currentSessionOrNull()?.refreshToken
    }

    override suspend fun refreshTokens(): BearerTokens? {
        return try {
            val session = supabase.auth.currentSessionOrNull()
            val refreshToken = session?.refreshToken
            if (refreshToken != null) {
                supabase.auth.refreshCurrentSession()
                val updatedSession = supabase.auth.currentSessionOrNull()
                if (updatedSession != null) {
                    BearerTokens(updatedSession.accessToken, updatedSession.refreshToken)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
