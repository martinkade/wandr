package com.wandr.domain.repository

import com.wandr.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfile(userId: String): Flow<Profile?>

    /** The cached profiles of [userIds] (those not cached yet are missing). */
    fun getProfiles(userIds: List<String>): Flow<List<Profile>>

    /** Pulls the remote profile into the local cache (never overwrites unsynced local edits). */
    suspend fun refreshProfile(userId: String): Result<Unit>

    /** Saves locally first; if the remote push fails the profile stays queued for [com.wandr.data.sync.SyncManager]. */
    suspend fun updateProfile(profile: Profile): Result<Profile>

    /** Uploads [jpegBytes] as the new avatar, links it to the profile and removes previous avatar files. */
    suspend fun setAvatar(userId: String, jpegBytes: ByteArray): Result<Profile>

    /** Clears the avatar on the profile and deletes the stored avatar files. */
    suspend fun removeAvatar(userId: String): Result<Profile>
}
