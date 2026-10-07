package com.wandr.domain.repository

import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamImageKind
import com.wandr.domain.model.TeamMember
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    fun getTeamById(teamId: String): Flow<Team?>
    fun getUserTeams(userId: String): Flow<List<Team>>

    /** The teams [userId] administers (owner or admin): only these can be enrolled in group challenges. */
    fun getAdminTeams(userId: String): Flow<List<Team>>

    /** Pulls the teams (and the user's memberships) visible to [userId] from Supabase into the local cache. */
    suspend fun refreshUserTeams(userId: String): Result<Unit>
    fun getTeamMembers(teamId: String): Flow<List<TeamMember>>
    suspend fun createTeam(name: String, description: String?, creatorId: String): Result<Team>

    /** Pulls the team and its members (with their profiles) into the local cache. Unsynced local edits are kept. */
    suspend fun refreshTeamDetails(teamId: String): Result<Unit>

    /** Saves name / description locally first; if the push fails the team stays queued for the sync manager. */
    suspend fun updateTeam(team: Team): Result<Team>

    /** Uploads [jpegBytes] as the team avatar or cover, links it to the team and removes previous files of that kind. */
    suspend fun setTeamImage(teamId: String, kind: TeamImageKind, jpegBytes: ByteArray): Result<Team>

    /** Clears the avatar or cover and deletes the stored files of that kind. */
    suspend fun removeTeamImage(teamId: String, kind: TeamImageKind): Result<Team>

    /**
     * Saves the priority order of the user's teams (first = highest). Needs the server, because it decides for which
     * team the user contributes to group challenges; the local cache follows once the server accepted the order.
     */
    suspend fun reorderTeams(userId: String, orderedTeamIds: List<String>): Result<Unit>

    suspend fun joinTeamViaInvite(inviteCode: String, userId: String): Result<Team>
    suspend fun generateInviteUrl(inviteCode: String): String
}
