package com.wandr.domain.repository

import com.wandr.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfile(userId: String): Flow<Profile?>
    suspend fun updateProfile(profile: Profile): Result<Profile>
    suspend fun uploadAvatar(userId: String, byteArray: ByteArray, fileName: String): Result<String>
}
