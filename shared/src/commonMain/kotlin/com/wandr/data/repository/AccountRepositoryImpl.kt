package com.wandr.data.repository

import com.wandr.data.fit.FitFileStorage
import com.wandr.data.local.dao.CacheDao
import com.wandr.data.remote.supabaseResult
import com.wandr.domain.repository.AccountRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable

@Serializable
private data class IdDto(val id: String)

/**
 * Account reset and deletion. The server does the work in `reset_my_data` / `delete_my_account` (SUPABASE.md, "Account
 * reset & deletion"); the files in Storage are deleted here first, because the Storage API is the only supported way
 * and its policies need the teams / challenges / user to still exist.
 */
class AccountRepositoryImpl(
    private val supabase: SupabaseClient,
    private val cacheDao: CacheDao,
    private val fitFileStorage: FitFileStorage
) : AccountRepository {

    override suspend fun resetData(userId: String): Result<Unit> = supabaseResult {
        deleteFilesOfOwnTeamsAndChallenges(userId)
        supabase.postgrest.rpc("reset_my_data")
        clearCache(keepUserId = userId)
    }

    override suspend fun deleteAccount(userId: String): Result<Unit> = supabaseResult {
        deleteFilesOfOwnTeamsAndChallenges(userId)
        deleteFolder(AVATAR_BUCKET, userId)
        supabase.postgrest.rpc("delete_my_account")
        // Nothing of the user may stay on this device; the session belongs to a user that no longer exists.
        clearCache(keepUserId = "")
        runCatching { supabase.auth.signOut(SignOutScope.LOCAL) }
        Unit
    }

    /** Covers and photos of the teams and challenges the user created; these go away with them on the server. */
    private suspend fun deleteFilesOfOwnTeamsAndChallenges(userId: String) {
        val teams = supabase.postgrest.from("teams")
            .select(Columns.list("id")) { filter { eq("created_by", userId) } }
            .decodeList<IdDto>()
        val challenges = supabase.postgrest.from("challenges")
            .select(Columns.list("id")) { filter { eq("created_by", userId) } }
            .decodeList<IdDto>()
        teams.forEach { deleteFolder(TEAM_BUCKET, it.id) }
        challenges.forEach { deleteFolder(CHALLENGE_BUCKET, it.id) }
    }

    /** Best effort: a file that cannot be deleted must not stop the reset or deletion of the data itself. */
    private suspend fun deleteFolder(bucketId: String, folder: String) {
        runCatching {
            val bucket = supabase.storage.from(bucketId)
            val names = bucket.list(folder).map { "$folder/${it.name}" }
            if (names.isNotEmpty()) bucket.delete(names)
        }
    }

    private suspend fun clearCache(keepUserId: String) {
        cacheDao.deleteActivities()
        cacheDao.deleteParticipants()
        cacheDao.deleteChallenges()
        cacheDao.deleteTeamMembers()
        cacheDao.deleteTeams()
        cacheDao.deleteProfilesExcept(keepUserId)
        fitFileStorage.deleteAll()
    }

    private companion object {
        const val AVATAR_BUCKET = "avatars"
        const val TEAM_BUCKET = "team-covers"
        const val CHALLENGE_BUCKET = "challenge-covers"
    }
}
