package com.wandr.data.repository

import com.wandr.data.local.dao.ChallengeDao
import com.wandr.data.local.dao.ChallengeParticipantDao
import com.wandr.data.local.entity.ChallengeEntity
import com.wandr.data.local.entity.ChallengeParticipantEntity
import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeParticipant
import com.wandr.domain.model.LeaderboardEntry
import com.wandr.domain.repository.ChallengeRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class ChallengeRepositoryImpl(
    private val challengeDao: ChallengeDao,
    private val participantDao: ChallengeParticipantDao,
    private val supabase: SupabaseClient
) : ChallengeRepository {

    override fun getChallengeById(challengeId: String): Flow<Challenge?> {
        return challengeDao.getChallengeById(challengeId).map { entity ->
            entity?.let {
                Challenge(
                    id = it.id,
                    teamId = it.teamId,
                    title = it.title,
                    description = it.description,
                    coverUrl = it.coverUrl,
                    scope = it.scope,
                    type = it.type,
                    targetValue = it.targetValue,
                    requireAllMembersCompletion = it.requireAllMembersCompletion,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    status = it.status,
                    createdBy = it.createdBy,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }

    override fun getTeamChallenges(teamId: String): Flow<List<Challenge>> {
        return challengeDao.getChallengesForTeam(teamId).map { list ->
            list.map {
                Challenge(
                    id = it.id,
                    teamId = it.teamId,
                    title = it.title,
                    description = it.description,
                    coverUrl = it.coverUrl,
                    scope = it.scope,
                    type = it.type,
                    targetValue = it.targetValue,
                    requireAllMembersCompletion = it.requireAllMembersCompletion,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    status = it.status,
                    createdBy = it.createdBy,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }

    override fun getChallengeParticipants(challengeId: String): Flow<List<ChallengeParticipant>> {
        return participantDao.getParticipantsForChallenge(challengeId).map { list ->
            list.map {
                ChallengeParticipant(
                    id = it.id,
                    challengeId = it.challengeId,
                    userId = it.userId,
                    progressValue = it.progressValue,
                    isCompleted = it.isCompleted,
                    joinedAt = it.joinedAt,
                    completedAt = it.completedAt
                )
            }
        }
    }

    override fun getPrivacyFirstLeaderboard(challengeId: String, teamId: String): Flow<List<LeaderboardEntry>> {
        return participantDao.getParticipantsForChallenge(challengeId).map { list ->
            list.mapIndexed { index, participant ->
                val challengeTarget = 100000.0 // target reference
                val percentage = ((participant.progressValue / challengeTarget) * 100).coerceAtMost(100.0)
                LeaderboardEntry(
                    rank = index + 1,
                    userId = participant.userId,
                    username = "user_${participant.userId.take(4)}",
                    displayName = "Athlete ${index + 1}",
                    avatarUrl = null,
                    progressValue = participant.progressValue,
                    progressPercentage = percentage,
                    isCompleted = participant.isCompleted
                )
            }
        }
    }

    override suspend fun createChallenge(challenge: Challenge): Result<Challenge> = runCatching {
        val entity = ChallengeEntity(
            id = challenge.id,
            teamId = challenge.teamId,
            title = challenge.title,
            description = challenge.description,
            coverUrl = challenge.coverUrl,
            scope = challenge.scope,
            type = challenge.type,
            targetValue = challenge.targetValue,
            requireAllMembersCompletion = challenge.requireAllMembersCompletion,
            startDate = challenge.startDate,
            endDate = challenge.endDate,
            status = challenge.status,
            createdBy = challenge.createdBy,
            createdAt = challenge.createdAt,
            updatedAt = challenge.updatedAt,
            syncStatus = "DIRTY"
        )

        // Local first write
        challengeDao.insertChallenge(entity)
        // Remote push
        supabase.postgrest.from("challenges").upsert(entity)
        challengeDao.insertChallenge(entity.copy(syncStatus = "SYNCED"))
        challenge
    }

    override suspend fun uploadChallengeCover(challengeId: String, bytes: ByteArray, fileName: String): Result<String> = runCatching {
        val bucket = supabase.storage.from("challenge-covers")
        val path = "$challengeId/$fileName"
        bucket.upload(path, bytes) { upsert = true }
        val publicUrl = bucket.publicUrl(path)
        publicUrl
    }

    override suspend fun joinChallenge(challengeId: String, userId: String): Result<Unit> = runCatching {
        val now = Clock.System.now().toEpochMilliseconds()
        val participantEntity = ChallengeParticipantEntity(
            id = "part_${challengeId}_$userId",
            challengeId = challengeId,
            userId = userId,
            progressValue = 0.0,
            isCompleted = false,
            joinedAt = now,
            syncStatus = "DIRTY"
        )
        participantDao.insertParticipant(participantEntity)
        supabase.postgrest.from("challenge_participants").upsert(participantEntity)
    }

    override suspend fun updateParticipantProgress(challengeId: String, userId: String, additionalProgress: Double): Result<Unit> = runCatching {
        val current = participantDao.getParticipant(challengeId, userId)
        if (current != null) {
            val newProgress = current.progressValue + additionalProgress
            val now = Clock.System.now().toEpochMilliseconds()
            val updated = current.copy(
                progressValue = newProgress,
                isCompleted = newProgress >= 100000.0,
                completedAt = if (newProgress >= 100000.0) now else current.completedAt,
                syncStatus = "DIRTY"
            )
            participantDao.insertParticipant(updated)
            supabase.postgrest.from("challenge_participants").upsert(updated)
        }
    }
}
