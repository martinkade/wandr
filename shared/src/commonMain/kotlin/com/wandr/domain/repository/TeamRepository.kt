package com.wandr.domain.repository

import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamMember
import kotlinx.coroutines.flow.Flow

interface TeamRepository {
    fun getTeamById(teamId: String): Flow<Team?>
    fun getUserTeams(userId: String): Flow<List<Team>>
    fun getTeamMembers(teamId: String): Flow<List<TeamMember>>
    suspend fun createTeam(name: String, description: String?, creatorId: String): Result<Team>
    suspend fun uploadTeamCover(teamId: String, bytes: ByteArray, fileName: String): Result<String>
    suspend fun joinTeamViaInvite(inviteCode: String, userId: String): Result<Team>
    suspend fun generateInviteUrl(inviteCode: String): String
}
