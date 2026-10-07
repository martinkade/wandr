package com.wandr.data.repository

import com.wandr.data.local.dao.ProfileDao
import com.wandr.data.local.dao.TeamDao
import com.wandr.data.local.dao.TeamMemberDao
import com.wandr.data.local.entity.TeamEntity
import com.wandr.data.local.entity.TeamMemberEntity
import com.wandr.data.remote.TeamDto
import com.wandr.data.remote.TeamMemberDto
import com.wandr.data.remote.TeamMemberWithProfileDto
import com.wandr.data.remote.supabaseResult
import com.wandr.data.remote.toDto
import com.wandr.data.remote.toUpdatePayload
import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamImageKind
import com.wandr.domain.model.TeamMember
import com.wandr.domain.model.TeamRole
import com.wandr.domain.repository.TeamRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TeamRepositoryImpl(
    private val teamDao: TeamDao,
    private val teamMemberDao: TeamMemberDao,
    private val profileDao: ProfileDao,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTeamMembers(teamId: String): Flow<List<TeamMember>> {
        return teamMemberDao.getMembersForTeam(teamId).flatMapLatest { members ->
            if (members.isEmpty()) {
                flowOf(emptyList())
            } else {
                profileDao.getProfilesByIds(members.map { it.userId }).map { profiles ->
                    val byId = profiles.associateBy { it.id }
                    members.map { member ->
                        val profile = byId[member.userId]
                        TeamMember(
                            id = member.id,
                            teamId = member.teamId,
                            userId = member.userId,
                            role = member.role,
                            username = profile?.username.orEmpty(),
                            displayName = profile?.displayName.orEmpty(),
                            avatarUrl = profile?.avatarUrl,
                            joinedAt = member.joinedAt
                        )
                    }.sortedWith(compareBy<TeamMember> { it.role.ordinal }.thenBy { it.displayName.lowercase() })
                }
            }
        }
    }

    override suspend fun refreshUserTeams(userId: String): Result<Unit> = supabaseResult {
        // RLS only returns teams the user is a member of, and the memberships of those teams.
        val teams = supabase.postgrest.from("teams").select().decodeList<TeamDto>()
        val memberships = supabase.postgrest.from("team_members").select {
            filter { eq("user_id", userId) }
        }.decodeList<TeamMemberDto>()
        teams.forEach { teamDao.insertTeam(it.toEntity()) }
        memberships.forEach { teamMemberDao.insertMember(it.toEntity()) }
    }

    /**
     * Creating a team needs the server: only managers may create teams (RLS), and the invite code must be unique.
     * So the remote insert happens first and the local cache is only written after it succeeded.
     */
    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createTeam(
        name: String,
        description: String?,
        creatorId: String
    ): Result<Team> = supabaseResult {
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = TeamEntity(
            id = Uuid.random().toString(),
            name = name,
            description = description?.takeIf { it.isNotBlank() },
            avatarUrl = null,
            coverUrl = null,
            inviteCode = (1..8).map { INVITE_CODE_ALPHABET.random() }.joinToString(""),
            createdBy = creatorId,
            createdAt = now,
            updatedAt = now
        )
        val memberEntity = TeamMemberEntity(
            id = Uuid.random().toString(),
            teamId = entity.id,
            userId = creatorId,
            role = TeamRole.ADMIN,
            joinedAt = now,
            // The server appends new memberships at the end of the list too.
            priority = teamMemberDao.maxPriority(creatorId) + 1
        )

        // The team must exist before its membership; the creator can only read it back once they are a member.
        supabase.postgrest.from("teams").insert(entity.toDto())
        supabase.postgrest.from("team_members").insert(memberEntity.toDto())

        teamDao.insertTeam(entity)
        teamMemberDao.insertMember(memberEntity)
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

    override suspend fun refreshTeamDetails(teamId: String): Result<Unit> = supabaseResult {
        val local = teamDao.getTeamOnce(teamId)
        if (local != null && local.syncStatus != SYNCED) {
            // Unsynced local edits win; try to push them instead of overwriting.
            supabaseResult { pushTeam(local) }.onSuccess { teamDao.insertTeam(local.copy(syncStatus = SYNCED)) }
        } else {
            supabase.postgrest.from("teams").select { filter { eq("id", teamId) } }
                .decodeSingleOrNull<TeamDto>()
                ?.let { teamDao.insertTeam(it.toEntity()) }
        }

        val rows = supabase.postgrest.from("team_members")
            .select(Columns.raw("id,team_id,user_id,role,joined_at,profiles(*)")) { filter { eq("team_id", teamId) } }
            .decodeList<TeamMemberWithProfileDto>()
        teamMemberDao.deleteMembersForTeam(teamId)
        rows.forEach { row ->
            teamMemberDao.insertMember(row.toMemberEntity())
            row.profiles?.let { dto ->
                // Never overwrite the signed-in user's own unsynced profile edits.
                val existing = profileDao.getProfileOnce(dto.id)
                if (existing == null || existing.syncStatus == SYNCED) profileDao.insertProfile(dto.toEntity())
            }
        }
    }

    override suspend fun updateTeam(team: Team): Result<Team> = supabaseResult {
        val existing = requireNotNull(teamDao.getTeamOnce(team.id)) { "Team is not loaded yet" }
        val entity = existing.copy(
            name = team.name,
            description = team.description?.takeIf { it.isNotBlank() },
            avatarUrl = team.avatarUrl,
            coverUrl = team.coverUrl,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            syncStatus = DIRTY
        )
        teamDao.insertTeam(entity) // local first
        supabaseResult { pushTeam(entity) }.onSuccess { teamDao.insertTeam(entity.copy(syncStatus = SYNCED)) }
        entity.toDomain()
    }

    override suspend fun setTeamImage(
        teamId: String,
        kind: TeamImageKind,
        jpegBytes: ByteArray
    ): Result<Team> = supabaseResult {
        val current = requireNotNull(teamDao.getTeamOnce(teamId)) { "Team is not loaded yet" }.toDomain()
        val fileName = "${kind.filePrefix}_${Clock.System.now().toEpochMilliseconds()}.jpg"
        val bucket = supabase.storage.from(TEAM_BUCKET)
        bucket.upload("$teamId/$fileName", jpegBytes) { contentType = ContentType.Image.JPEG }
        val url = bucket.publicUrl("$teamId/$fileName")

        val updated = updateTeam(
            when (kind) {
                TeamImageKind.AVATAR -> current.copy(avatarUrl = url)
                TeamImageKind.COVER -> current.copy(coverUrl = url)
            }
        ).getOrThrow()
        // Best effort: stale files are harmless, a failed cleanup must not fail the update.
        supabaseResult { deleteImages(teamId, kind, keepFileName = fileName) }
        updated
    }

    override suspend fun removeTeamImage(teamId: String, kind: TeamImageKind): Result<Team> =
        supabaseResult {
        val current = requireNotNull(teamDao.getTeamOnce(teamId)) { "Team is not loaded yet" }.toDomain()
        val updated = updateTeam(
            when (kind) {
                TeamImageKind.AVATAR -> current.copy(avatarUrl = null)
                TeamImageKind.COVER -> current.copy(coverUrl = null)
            }
        ).getOrThrow()
            supabaseResult { deleteImages(teamId, kind, keepFileName = null) }
        updated
    }

    private suspend fun pushTeam(entity: TeamEntity) {
        supabase.postgrest.from("teams").update(entity.toUpdatePayload()) { filter { eq("id", entity.id) } }
    }

    private suspend fun deleteImages(teamId: String, kind: TeamImageKind, keepFileName: String?) {
        val bucket = supabase.storage.from(TEAM_BUCKET)
        val stale = bucket.list(teamId).map { it.name }
            .filter { it.startsWith("${kind.filePrefix}_") && it != keepFileName }
        if (stale.isNotEmpty()) bucket.delete(stale.map { "$teamId/$it" })
    }

    private fun TeamEntity.toDomain() = Team(
        id = id, name = name, description = description, avatarUrl = avatarUrl, coverUrl = coverUrl,
        inviteCode = inviteCode, createdBy = createdBy, createdAt = createdAt, updatedAt = updatedAt
    )

    /**
     * Joining goes through a server function: a non-member cannot read a team (RLS), so the code cannot be looked up
     * from the client. The server also appends the new membership at the end of the user's priority list.
     */
    override suspend fun joinTeamViaInvite(inviteCode: String, userId: String): Result<Team> =
        supabaseResult {
        val remoteTeam = supabase.postgrest
            .rpc("join_team_by_invite", buildJsonObject { put("p_invite_code", inviteCode) })
            .decodeAs<TeamDto>()
            .toEntity()

        teamDao.insertTeam(remoteTeam)
        // Pulls the new membership (with its priority); the team itself is already cached.
        refreshUserTeams(userId)

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

    override suspend fun reorderTeams(userId: String, orderedTeamIds: List<String>): Result<Unit> =
        supabaseResult {
        supabase.postgrest.rpc("set_team_priorities", buildJsonObject {
            put("p_team_ids", JsonArray(orderedTeamIds.map { JsonPrimitive(it) }))
        })
        orderedTeamIds.forEachIndexed { index, teamId -> teamMemberDao.setPriority(teamId, userId, index) }
    }

    override suspend fun generateInviteUrl(inviteCode: String): String {
        return "wandr://invite/$inviteCode"
    }

    private companion object {
        const val INVITE_CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        const val TEAM_BUCKET = "team-covers"
        const val SYNCED = "SYNCED"
        const val DIRTY = "DIRTY"
    }
}
