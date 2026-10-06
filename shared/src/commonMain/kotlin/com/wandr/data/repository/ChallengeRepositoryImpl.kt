package com.wandr.data.repository

import com.wandr.data.local.dao.ChallengeDao
import com.wandr.data.local.dao.ChallengeParticipantDao
import com.wandr.data.local.entity.ChallengeEntity
import com.wandr.data.local.entity.ChallengeParticipantEntity
import com.wandr.data.remote.ChallengeDto
import com.wandr.data.remote.ChallengeParticipantDto
import com.wandr.data.remote.ChallengeParticipantInsertDto
import com.wandr.data.remote.ChallengeTeamInsertDto
import com.wandr.data.remote.MemberRankingDto
import com.wandr.data.remote.TeamStandingDto
import com.wandr.data.remote.toDto
import com.wandr.data.remote.toUpdatePayload
import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeParticipant
import com.wandr.domain.model.ChallengeParticipation
import com.wandr.domain.model.ChallengeStatus
import com.wandr.domain.model.LeaderboardEntry
import com.wandr.domain.model.TeamStanding
import com.wandr.domain.repository.ChallengeRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ChallengeRepositoryImpl(
    private val challengeDao: ChallengeDao,
    private val participantDao: ChallengeParticipantDao,
    private val supabase: SupabaseClient
) : ChallengeRepository {

    override fun getChallengeById(challengeId: String): Flow<Challenge?> =
        challengeDao.getChallengeById(challengeId).map { it?.toDomain() }

    override fun getChallenges(): Flow<List<Challenge>> =
        challengeDao.getAllChallenges().map { list -> list.map { it.toDomain() } }

    override fun getChallengeParticipants(challengeId: String): Flow<List<ChallengeParticipant>> {
        return participantDao.getParticipantsForChallenge(challengeId).map { list ->
            list.map {
                ChallengeParticipant(
                    id = it.id,
                    challengeId = it.challengeId,
                    userId = it.userId,
                    teamId = it.teamId,
                    progressValue = it.progressValue,
                    isCompleted = it.isCompleted,
                    joinedAt = it.joinedAt,
                    completedAt = it.completedAt
                )
            }
        }
    }

    override suspend fun getMemberRanking(challengeId: String): Result<List<LeaderboardEntry>> = runCatching {
        val target = challengeDao.getChallengeOnce(challengeId)?.targetValue?.takeIf { it > 0.0 }
        val rows = supabase.postgrest.rpc("challenge_member_ranking", buildJsonObject { put("p_challenge_id", challengeId) })
            .decodeList<MemberRankingDto>()
            .sortedByDescending { it.progressValue }
        rows.mapIndexed { index, row ->
            LeaderboardEntry(
                // Equal progress shares the rank of the first of them.
                rank = rows.indexOfFirst { it.progressValue == row.progressValue } + 1,
                userId = row.userId,
                displayName = row.displayName.orEmpty(),
                avatarUrl = row.avatarUrl,
                progressValue = row.progressValue,
                progressPercentage = target?.let { ((row.progressValue / it) * 100).coerceAtMost(100.0) } ?: 0.0,
                isCompleted = row.isCompleted
            )
        }
    }

    override suspend fun getTeamStandings(challengeId: String): Result<List<TeamStanding>> = runCatching {
        val challenge = challengeDao.getChallengeOnce(challengeId)
        val target = challenge?.targetValue?.takeIf { it > 0.0 }
        val requireAll = challenge?.requireAllMembersCompletion ?: false
        supabase.postgrest.rpc("challenge_team_standings", buildJsonObject { put("p_challenge_id", challengeId) })
            .decodeList<TeamStandingDto>()
            .sortedByDescending { it.totalProgress }
            .mapIndexed { index, row ->
                TeamStanding(
                    rank = index + 1,
                    teamId = row.teamId,
                    teamName = row.teamName,
                    avatarUrl = row.teamAvatarUrl,
                    totalProgress = row.totalProgress,
                    progressPercentage = target?.let { ((row.totalProgress / it) * 100).coerceAtMost(100.0) } ?: 0.0,
                    memberCount = row.memberCount,
                    completedMemberCount = row.completedCount,
                    isCompleted = if (requireAll) row.memberCount > 0 && row.completedCount == row.memberCount
                    else row.completedCount > 0
                )
            }
    }

    override suspend fun refreshChallenges(userId: String): Result<Unit> = runCatching {
        // RLS only returns what the user may see. Unsynced local edits are never overwritten.
        supabase.postgrest.from("challenges").select().decodeList<ChallengeDto>().forEach { dto ->
            val local = challengeDao.getChallengeOnce(dto.id)
            if (local == null || local.syncStatus == SYNCED) challengeDao.insertChallenge(dto.toEntity())
        }

        // Own participations (team members' rows are visible too, so filter by user). Synced rows that no longer
        // exist on the server (left / team withdrawn) are removed locally.
        val mine = supabase.postgrest.from("challenge_participants")
            .select { filter { eq("user_id", userId) } }
            .decodeList<ChallengeParticipantDto>()
        val remoteIds = mine.map { it.id }.toSet()
        participantDao.getParticipationsForUserOnce(userId)
            .filter { it.syncStatus == SYNCED && it.id !in remoteIds }
            .forEach { participantDao.deleteParticipantById(it.id) }
        mine.forEach { participantDao.insertParticipant(it.toEntity()) }
    }

    override fun getParticipations(userId: String): Flow<List<ChallengeParticipation>> =
        participantDao.getParticipationsForUser(userId).map { rows ->
            rows.map { ChallengeParticipation(challengeId = it.challengeId, teamId = it.teamId) }
        }

    /**
     * Only managers may create challenges (RLS), so the remote insert happens first; the local cache is only
     * written after it succeeded. [Challenge.id] must already be a UUID.
     */
    override suspend fun createChallenge(challenge: Challenge): Result<Challenge> = runCatching {
        val entity = challenge.toEntity()
        supabase.postgrest.from("challenges").insert(entity.toDto())
        challengeDao.insertChallenge(entity)
        challenge
    }

    override suspend fun updateChallenge(challenge: Challenge): Result<Challenge> = runCatching {
        requireNotNull(challengeDao.getChallengeOnce(challenge.id)) { "Challenge is not loaded yet" }
        val entity = challenge.toEntity(syncStatus = DIRTY)
            .copy(updatedAt = Clock.System.now().toEpochMilliseconds())
        challengeDao.insertChallenge(entity) // local first
        runCatching { pushChallenge(entity) }.onSuccess { challengeDao.insertChallenge(entity.copy(syncStatus = SYNCED)) }
        entity.toDomain()
    }

    private suspend fun pushChallenge(entity: ChallengeEntity) {
        supabase.postgrest.from("challenges").update(entity.toUpdatePayload()) { filter { eq("id", entity.id) } }
    }

    private fun Challenge.toEntity(syncStatus: String = SYNCED) = ChallengeEntity(
        id = id, title = title, description = description, coverUrl = coverUrl, scope = scope,
        type = type, targetValue = targetValue, requireAllMembersCompletion = requireAllMembersCompletion,
        activityTypes = activityTypes.joinToString(","),
        startDate = startDate, endDate = endDate,
        status = ChallengeStatus.storedValue(isActive),
        createdBy = createdBy, createdAt = createdAt, updatedAt = updatedAt, syncStatus = syncStatus
    )

    private fun ChallengeEntity.toDomain() = Challenge(
        id = id, title = title, description = description, coverUrl = coverUrl, scope = scope,
        type = type, targetValue = targetValue, requireAllMembersCompletion = requireAllMembersCompletion,
        activityTypes = activityTypes.split(",").filter { it.isNotBlank() },
        startDate = startDate, endDate = endDate, createdBy = createdBy, createdAt = createdAt,
        updatedAt = updatedAt, isActive = status == ChallengeStatus.ACTIVE.value
    )

    override suspend fun setChallengeCover(challengeId: String, jpegBytes: ByteArray): Result<Challenge> = runCatching {
        val current = requireNotNull(challengeDao.getChallengeOnce(challengeId)) { "Challenge is not loaded yet" }.toDomain()
        val fileName = "cover_${Clock.System.now().toEpochMilliseconds()}.jpg"
        val bucket = supabase.storage.from(COVER_BUCKET)
        bucket.upload("$challengeId/$fileName", jpegBytes) { contentType = ContentType.Image.JPEG }
        val updated = updateChallenge(current.copy(coverUrl = bucket.publicUrl("$challengeId/$fileName"))).getOrThrow()
        // Best effort: stale files are harmless, a failed cleanup must not fail the update.
        runCatching { deleteCovers(challengeId, keepFileName = fileName) }
        updated
    }

    override suspend fun removeChallengeCover(challengeId: String): Result<Challenge> = runCatching {
        val current = requireNotNull(challengeDao.getChallengeOnce(challengeId)) { "Challenge is not loaded yet" }.toDomain()
        val updated = updateChallenge(current.copy(coverUrl = null)).getOrThrow()
        runCatching { deleteCovers(challengeId, keepFileName = null) }
        updated
    }

    private suspend fun deleteCovers(challengeId: String, keepFileName: String?) {
        val bucket = supabase.storage.from(COVER_BUCKET)
        val stale = bucket.list(challengeId).map { it.name }.filter { it.startsWith("cover_") && it != keepFileName }
        if (stale.isNotEmpty()) bucket.delete(stale.map { "$challengeId/$it" })
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun joinChallenge(challengeId: String, userId: String): Result<Unit> = runCatching {
        val challenge = challengeDao.getChallengeOnce(challengeId)
        require(challenge?.scope != "group") { "Group challenges are joined by enrolling a team" }
        val now = Clock.System.now().toEpochMilliseconds()
        val participant = ChallengeParticipantEntity(
            id = Uuid.random().toString(),
            challengeId = challengeId,
            userId = userId,
            teamId = null,
            progressValue = 0.0,
            isCompleted = false,
            joinedAt = now
        )
        supabase.postgrest.from("challenge_participants")
            .insert(ChallengeParticipantInsertDto(participant.id, challengeId, userId))
        participantDao.insertParticipant(participant)
    }

    override suspend fun leaveChallenge(challengeId: String, userId: String): Result<Unit> = runCatching {
        // RLS: users may only quit individual challenges themselves.
        supabase.postgrest.from("challenge_participants").delete {
            filter {
                eq("challenge_id", challengeId)
                eq("user_id", userId)
            }
        }
        participantDao.deleteParticipant(challengeId, userId)
    }

    override suspend fun withdrawTeam(challengeId: String, teamId: String): Result<Unit> = runCatching {
        // Server-side (RLS): only the team owner or an admin may do this. A trigger removes the members.
        supabase.postgrest.from("challenge_teams").delete {
            filter {
                eq("challenge_id", challengeId)
                eq("team_id", teamId)
            }
        }
        participantDao.deleteTeamParticipants(challengeId, teamId)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun enrollTeam(challengeId: String, teamId: String, enrolledBy: String): Result<Unit> = runCatching {
        val challenge = challengeDao.getChallengeOnce(challengeId)
        require(challenge?.scope == "group") { "Only group challenges can be joined by a team" }
        // Server-side (RLS): only the team owner or an admin may do this. A trigger adds all members as participants.
        supabase.postgrest.from("challenge_teams")
            .insert(ChallengeTeamInsertDto(Uuid.random().toString(), challengeId, teamId, enrolledBy))
    }

    private companion object {
        const val SYNCED = "SYNCED"
        const val DIRTY = "DIRTY"
        const val COVER_BUCKET = "challenge-covers"
    }
}
