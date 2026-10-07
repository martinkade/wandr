package com.wandr.presentation.team

import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem
import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamImageKind
import com.wandr.domain.model.TeamMember
import com.wandr.domain.repository.TeamRepository
import com.wandr.domain.usecase.CreateTeamUseCase
import com.wandr.domain.usecase.GetUserTeamsUseCase
import com.wandr.domain.usecase.JoinTeamViaInviteUseCase
import com.wandr.domain.usecase.RefreshUserTeamsUseCase
import com.wandr.domain.usecase.ReorderTeamsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun team(id: String) = Team(id, "Team $id", null, null, null, "CODE$id", "owner", 0L, 0L)

private class OrderingTeamRepository(initial: List<Team>) : TeamRepository {
    val teams = MutableStateFlow(initial)
    var failReorder = false
    var joinedWith: String? = null
    var savedOrder: List<String>? = null

    override fun getUserTeams(userId: String): Flow<List<Team>> = teams
    override suspend fun reorderTeams(userId: String, orderedTeamIds: List<String>): Result<Unit> {
        if (failReorder) return Result.failure(IllegalStateException("offline"))
        savedOrder = orderedTeamIds
        teams.value = orderedTeamIds.map { id -> teams.value.first { it.id == id } }
        return Result.success(Unit)
    }
    override suspend fun joinTeamViaInvite(inviteCode: String, userId: String): Result<Team> {
        joinedWith = inviteCode
        return Result.success(team("new"))
    }
    override fun getTeamById(teamId: String): Flow<Team?> = flowOf(null)
    override suspend fun refreshUserTeams(userId: String) = Result.success(Unit)
    override fun getTeamMembers(teamId: String): Flow<List<TeamMember>> = flowOf(emptyList())
    override suspend fun createTeam(name: String, description: String?, creatorId: String): Result<Team> = Result.failure(UnsupportedOperationException())
    override suspend fun refreshTeamDetails(teamId: String) = Result.success(Unit)
    override suspend fun updateTeam(team: Team): Result<Team> = Result.success(team)
    override suspend fun setTeamImage(teamId: String, kind: TeamImageKind, jpegBytes: ByteArray): Result<Team> = Result.failure(UnsupportedOperationException())
    override suspend fun removeTeamImage(teamId: String, kind: TeamImageKind): Result<Team> = Result.failure(UnsupportedOperationException())
    override suspend fun generateInviteUrl(inviteCode: String) = "wandr://invite/$inviteCode"
}

@OptIn(ExperimentalCoroutinesApi::class)
class TeamViewModelReorderTest {
    private fun kotlinx.coroutines.test.TestScope.vm(repo: OrderingTeamRepository) = TeamViewModel(
        GetUserTeamsUseCase(repo), CreateTeamUseCase(repo), JoinTeamViaInviteUseCase(repo), RefreshUserTeamsUseCase(repo),
        ReorderTeamsUseCase(repo), repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler))
    ).also { it.processIntent(TeamIntent.LoadUserTeams("me")) }

    @Test
    fun newOrderIsSavedAndShown() = runTest {
        val repo = OrderingTeamRepository(listOf(team("a"), team("b"), team("c")))
        val viewModel = vm(repo)
        viewModel.processIntent(TeamIntent.ReorderTeams("me", listOf("c", "a", "b")))

        assertEquals(listOf("c", "a", "b"), repo.savedOrder)
        assertEquals(listOf("c", "a", "b"), viewModel.uiState.value.teams.map { it.id })
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun rejectedOrderIsRevertedWithAnError() = runTest {
        val repo = OrderingTeamRepository(listOf(team("a"), team("b"))).apply { failReorder = true }
        val viewModel = vm(repo)
        viewModel.processIntent(TeamIntent.ReorderTeams("me", listOf("b", "a")))

        assertEquals(listOf("a", "b"), viewModel.uiState.value.teams.map { it.id })
        assertEquals("offline", viewModel.uiState.value.error?.cause?.message)
    }

    @Test
    fun unchangedOrderDoesNotCallTheServer() = runTest {
        val repo = OrderingTeamRepository(listOf(team("a"), team("b")))
        val viewModel = vm(repo)
        viewModel.processIntent(TeamIntent.ReorderTeams("me", listOf("a", "b")))
        assertNull(repo.savedOrder)
    }

    @Test
    fun joinAcceptsTheContentOfAnInviteQrCode() = runTest {
        val repo = OrderingTeamRepository(listOf(team("a")))
        val viewModel = vm(repo)
        viewModel.processIntent(TeamIntent.JoinInviteCodeChanged("wandr://invite/X7K9P2W1"))
        viewModel.processIntent(TeamIntent.SubmitJoinTeam("me"))
        assertEquals("X7K9P2W1", repo.joinedWith)
        assertTrue(viewModel.uiState.value.successMessage != null)
    }

    @Test
    fun invalidInviteCodeIsRejectedWithoutServerCall() = runTest {
        val repo = OrderingTeamRepository(listOf(team("a")))
        val viewModel = vm(repo)
        viewModel.processIntent(TeamIntent.JoinInviteCodeChanged("no code!"))
        viewModel.processIntent(TeamIntent.SubmitJoinTeam("me"))
        assertNull(repo.joinedWith)
        assertIs<AppError.InvalidInput>(viewModel.uiState.value.error)
        assertEquals(
            InputProblem.INVITE_CODE_INVALID,
            (viewModel.uiState.value.error as AppError.InvalidInput).problem
        )
    }
}
