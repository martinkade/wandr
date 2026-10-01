package com.wandr.data.repository

import com.wandr.data.local.dao.ProfileDao
import com.wandr.data.local.entity.ProfileEntity
import com.wandr.domain.model.Profile
import com.wandr.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao,
    private val supabase: SupabaseClient
) : ProfileRepository {

    override fun getProfile(userId: String): Flow<Profile?> {
        return profileDao.getProfileById(userId).map { entity ->
            entity?.let {
                Profile(
                    id = it.id,
                    username = it.username,
                    displayName = it.displayName,
                    avatarUrl = it.avatarUrl,
                    bio = it.bio,
                    systemRole = it.systemRole,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }

    override suspend fun updateProfile(profile: Profile): Result<Profile> = runCatching {
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = ProfileEntity(
            id = profile.id,
            username = profile.username,
            displayName = profile.displayName,
            avatarUrl = profile.avatarUrl,
            bio = profile.bio,
            systemRole = profile.systemRole,
            createdAt = profile.createdAt,
            updatedAt = now,
            syncStatus = "DIRTY"
        )
        // Local first write
        profileDao.insertProfile(entity)

        // Remote push to Supabase
        supabase.postgrest.from("profiles").upsert(entity)
        profileDao.insertProfile(entity.copy(syncStatus = "SYNCED"))
        profile.copy(updatedAt = now)
    }

    override suspend fun uploadAvatar(userId: String, byteArray: ByteArray, fileName: String): Result<String> = runCatching {
        val bucket = supabase.storage.from("avatars")
        val path = "$userId/$fileName"
        bucket.upload(path, byteArray) { upsert = true }
        val publicUrl = bucket.publicUrl(path)
        publicUrl
    }
}
