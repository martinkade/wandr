package com.wandr.domain.repository

interface PushTokenRepository {
    /** Links the device token to the signed-in user (moves it if another user had it on a shared device). */
    suspend fun register(token: String, platform: String): Result<Unit>
    suspend fun unregister(token: String): Result<Unit>
}
