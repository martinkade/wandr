package com.wandr.domain.usecase

import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamImageKind
import com.wandr.domain.model.TeamMember
import com.wandr.domain.repository.TeamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class RecordingTeamRepository : TeamRepository {
    var created: Triple<String, String?, String>? = null
    var refreshedFor: String? = null

    override fun getTeamById(teamId: String): Flow<Team?> = emptyFlow()
    override fun getUserTeams(userId: String): Flow<List<Team>> = emptyFlow()
    override suspend fun refreshUserTeams(userId: String): Result<Unit> {
        refreshedFor = userId
        return Result.success(Unit)
    }
    override fun getTeamMembers(teamId: String): Flow<List<TeamMember>> = emptyFlow()
    override suspend fun createTeam(name: String, description: String?, creatorId: String): Result<Team> {
        created = Triple(name, description, creatorId)
        return Result.success(Team("t1", name, description, null, null, "ABCD1234", creatorId, 0, 0))
    }
    override suspend fun refreshTeamDetails(teamId: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateTeam(team: Team): Result<Team> = Result.success(team)
    override suspend fun setTeamImage(teamId: String, kind: TeamImageKind, jpegBytes: ByteArray): Result<Team> =
        Result.failure(UnsupportedOperationException())
    override suspend fun removeTeamImage(teamId: String, kind: TeamImageKind): Result<Team> =
        Result.failure(UnsupportedOperationException())
    override suspend fun joinTeamViaInvite(inviteCode: String, userId: String): Result<Team> =
        Result.failure(UnsupportedOperationException())
    override suspend fun generateInviteUrl(inviteCode: String): String = "wandr://invite/$inviteCode"
}

class CreateTeamUseCaseTest {

    @Test
    fun blankNameIsRejectedWithoutCallingRepository() = runTest {
        val repo = RecordingTeamRepository()
        val result = CreateTeamUseCase(repo)("   ", "desc", "u1")
        assertTrue(result.isFailure)
        assertEquals(null, repo.created)
    }

    @Test
    fun nameAndDescriptionAreTrimmed() = runTest {
        val repo = RecordingTeamRepository()
        val team = CreateTeamUseCase(repo)("  Trail Blazers ", " Weekend hikes ", "u1").getOrThrow()
        assertEquals("Trail Blazers", team.name)
        assertEquals(Triple("Trail Blazers", "Weekend hikes", "u1"), repo.created)
    }

    @Test
    fun refreshDelegatesToRepository() = runTest {
        val repo = RecordingTeamRepository()
        RefreshUserTeamsUseCase(repo)("u7")
        assertEquals("u7", repo.refreshedFor)
    }
}
