package com.wandr.data.repository

import com.wandr.domain.repository.PushTokenRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class PushTokenRepositoryImpl(private val supabase: SupabaseClient) : PushTokenRepository {

    // An RPC instead of an upsert: the token may still belong to the previous user of a shared device.
    override suspend fun register(token: String, platform: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc("register_device_token", buildJsonObject {
            put("p_token", token)
            put("p_platform", platform)
        })
        Unit
    }

    override suspend fun unregister(token: String): Result<Unit> = runCatching {
        supabase.postgrest.from("device_tokens").delete { filter { eq("token", token) } }
        Unit
    }
}
