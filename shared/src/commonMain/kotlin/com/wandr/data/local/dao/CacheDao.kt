package com.wandr.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query

/** Empties the local cache (account reset / deletion). */
@Dao
interface CacheDao {
    @Query("DELETE FROM activities")
    suspend fun deleteActivities()

    @Query("DELETE FROM challenge_participants")
    suspend fun deleteParticipants()

    @Query("DELETE FROM challenges")
    suspend fun deleteChallenges()

    @Query("DELETE FROM team_members")
    suspend fun deleteTeamMembers()

    @Query("DELETE FROM teams")
    suspend fun deleteTeams()

    /** All cached profiles except [keepUserId] (the signed-in user's own one survives a reset). */
    @Query("DELETE FROM profiles WHERE id != :keepUserId")
    suspend fun deleteProfilesExcept(keepUserId: String)
}
