package com.wandr.data.repository

import com.wandr.data.local.dao.ProfileDao
import com.wandr.data.local.entity.ProfileEntity
import com.wandr.data.remote.ProfileRemoteDataSource
import com.wandr.domain.model.Profile
import com.wandr.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao,
    private val remote: ProfileRemoteDataSource
) : ProfileRepository {

    override fun getProfile(userId: String): Flow<Profile?> =
        profileDao.getProfileById(userId).map { it?.toDomain() }

    override suspend fun refreshProfile(userId: String): Result<Unit> = runCatching {
        val local = profileDao.getProfileOnce(userId)
        if (local != null && local.syncStatus != SYNCED) {
            // Unsynced local edits win; try to push them instead.
            runCatching { remote.push(local) }.onSuccess { profileDao.insertProfile(local.copy(syncStatus = SYNCED)) }
            return@runCatching
        }
        remote.fetch(userId)?.let { profileDao.insertProfile(it.toEntity()) }
    }

    override suspend fun updateProfile(profile: Profile): Result<Profile> = runCatching {
        val existing = profileDao.getProfileOnce(profile.id)
        val entity = ProfileEntity(
            id = profile.id,
            username = existing?.username ?: profile.username,
            displayName = profile.displayName,
            avatarUrl = profile.avatarUrl,
            bio = profile.bio?.takeIf { it.isNotBlank() },
            systemRole = existing?.systemRole ?: profile.systemRole,
            createdAt = existing?.createdAt ?: profile.createdAt,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            syncStatus = DIRTY
        )
        profileDao.insertProfile(entity) // local first
        runCatching { remote.push(entity) }
            .onSuccess { profileDao.insertProfile(entity.copy(syncStatus = SYNCED)) }
        entity.toDomain()
    }

    override suspend fun setAvatar(userId: String, jpegBytes: ByteArray): Result<Profile> = runCatching {
        val current = requireNotNull(profileDao.getProfileOnce(userId)) { "Profile is not loaded yet" }
        val fileName = "avatar_${Clock.System.now().toEpochMilliseconds()}.jpg"
        val url = remote.uploadAvatar(userId, jpegBytes, fileName)
        val updated = updateProfile(current.toDomain().copy(avatarUrl = url)).getOrThrow()
        // Best effort: stale files are harmless, a failed cleanup must not fail the update.
        runCatching { remote.deleteAvatars(userId, keepFileName = fileName) }
        updated
    }

    override suspend fun removeAvatar(userId: String): Result<Profile> = runCatching {
        val current = requireNotNull(profileDao.getProfileOnce(userId)) { "Profile is not loaded yet" }
        val updated = updateProfile(current.toDomain().copy(avatarUrl = null)).getOrThrow()
        runCatching { remote.deleteAvatars(userId) }
        updated
    }

    private fun ProfileEntity.toDomain() = Profile(
        id = id,
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        bio = bio,
        systemRole = systemRole,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private companion object {
        const val SYNCED = "SYNCED"
        const val DIRTY = "DIRTY"
    }
}
