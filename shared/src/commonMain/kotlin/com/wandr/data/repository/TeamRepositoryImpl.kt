package com.wandr.data.repository

import com.wandr.data.local.dao.TeamDao
import com.wandr.data.local.dao.TeamMemberDao
import com.wandr.data.local.entity.TeamEntity
import com.wandr.data.local.entity.TeamMemberEntity
import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamMember
import com.wandr.domain.repository.TeamRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class TeamRepositoryImpl(
    private val teamDao: TeamDao,
    private val teamMemberDao: TeamMemberDao,
    private val supabase: SupabaseClient
) : TeamRepository {

    override fun getTeamById(teamId: String): Flow<Team?> {
        return teamDao.getTeamById(teamId).map { entity ->
            entity?.let {
                Team(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    avatarUrl = it.avatarUrl,
                    coverUrl = it.coverUrl,
                    inviteCode = it.inviteCode,
                    createdBy = it.createdBy,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }

    override fun getUserTeams(userId: String): Flow<List<Team>> {
        return teamDao.getTeamsForUser(userId).map { list ->
            list.map {
                Team(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    avatarUrl = it.avatarUrl,
                    coverUrl = it.coverUrl,
                    inviteCode = it.inviteCode,
                    createdBy = it.createdBy,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }

    override fun getTeamMembers(teamId: String): Flow<List<TeamMember>> {
        return teamMemberDao.getMembersForTeam(teamId).map { list ->
            list.map {
                TeamMember(
                    id = it.id,
                    teamId = it.teamId,
                    userId = it.userId,
                    role = it.role,
                    username = "member",
                    displayName = "Team Member",
                    avatarUrl = null,
                    joinedAt = it.joinedAt
                )
            }
        }
    }

    override suspend fun createTeam(name: String, description: String?, creatorId: String): Result<Team> = runCatching {
        val now = Clock.System.now().toEpochMilliseconds()
        val randomCode = (1..8).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
        val teamId = "team_$now"

        val entity = TeamEntity(
            id = teamId,
            name = name,
            description = description,
            avatarUrl = null,
            coverUrl = null,
            inviteCode = randomCode,
            createdBy = creatorId,
            createdAt = now,
            updatedAt = now,
            syncStatus = "DIRTY"
        )
        // Local first write
        teamDao.insertTeam(entity)

        val memberEntity = TeamMemberEntity(
            id = "member_${teamId}_$creatorId",
            teamId = teamId,
            userId = creatorId,
            role = "admin",
            joinedAt = now,
            syncStatus = "DIRTY"
        )
        teamMemberDao.insertMember(memberEntity)

        // Remote push
        supabase.postgrest.from("teams").upsert(entity)
        supabase.postgrest.from("team_members").upsert(memberEntity)

        teamDao.insertTeam(entity.copy(syncStatus = "SYNCED"))
        Team(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            avatarUrl = entity.avatarUrl,
            coverUrl = entity.coverUrl,
            inviteCode = entity.inviteCode,
            createdBy = entity.createdBy,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun uploadTeamCover(teamId: String, bytes: ByteArray, fileName: String): Result<String> = runCatching {
        val bucket = supabase.storage.from("team-covers")
        val path = "$teamId/$fileName"
        bucket.upload(path, bytes) { upsert = true }
        val publicUrl = bucket.publicUrl(path)
        publicUrl
    }

    override suspend fun joinTeamViaInvite(inviteCode: String, userId: String): Result<Team> = runCatching {
        val remoteTeam = supabase.postgrest.from("teams")
            .select { filter { eq("invite_code", inviteCode) } }
            .decodeSingle<TeamEntity>()

        val now = Clock.System.now().toEpochMilliseconds()
        val memberEntity = TeamMemberEntity(
            id = "member_${remoteTeam.id}_$userId",
            teamId = remoteTeam.id,
            userId = userId,
            role = "member",
            joinedAt = now,
            syncStatus = "DIRTY"
        )

        teamDao.insertTeam(remoteTeam)
        teamMemberDao.insertMember(memberEntity)
        supabase.postgrest.from("team_members").upsert(memberEntity)

        Team(
            id = remoteTeam.id,
            name = remoteTeam.name,
            description = remoteTeam.description,
            avatarUrl = remoteTeam.avatarUrl,
            coverUrl = remoteTeam.coverUrl,
            inviteCode = remoteTeam.inviteCode,
            createdBy = remoteTeam.createdBy,
            createdAt = remoteTeam.createdAt,
            updatedAt = remoteTeam.updatedAt
        )
    }

    override suspend fun generateInviteUrl(inviteCode: String): String {
        return "wandr://invite/$inviteCode"
    }
}
