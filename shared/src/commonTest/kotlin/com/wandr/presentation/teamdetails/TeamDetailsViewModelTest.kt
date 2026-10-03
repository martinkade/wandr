package com.wandr.presentation.teamdetails

import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamImageKind
import com.wandr.domain.model.TeamMember
import com.wandr.domain.model.TeamRole
import com.wandr.domain.repository.TeamRepository
import com.wandr.domain.usecase.GetTeamMembersUseCase
import com.wandr.domain.usecase.GetTeamUseCase
import com.wandr.domain.usecase.RefreshTeamDetailsUseCase
import com.wandr.domain.usecase.RemoveTeamImageUseCase
import com.wandr.domain.usecase.SetTeamImageUseCase
import com.wandr.domain.usecase.UpdateTeamUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeTeamRepository(initial: Team?, members: List<TeamMember>) : TeamRepository {
    val team = MutableStateFlow(initial)
    val memberList = MutableStateFlow(members)
    var refreshed = 0
    var failImages = false

    override fun getTeamById(teamId: String): Flow<Team?> = team
    override fun getUserTeams(userId: String): Flow<List<Team>> = emptyFlow()
    override suspend fun refreshUserTeams(userId: String): Result<Unit> = Result.success(Unit)
    override fun getTeamMembers(teamId: String): Flow<List<TeamMember>> = memberList
    override suspend fun createTeam(name: String, description: String?, creatorId: String): Result<Team> =
        Result.failure(UnsupportedOperationException())
    override suspend fun refreshTeamDetails(teamId: String): Result<Unit> {
        refreshed++
        return Result.success(Unit)
    }
    override suspend fun updateTeam(team: Team): Result<Team> {
        this.team.value = team
        return Result.success(team)
    }
    override suspend fun setTeamImage(teamId: String, kind: TeamImageKind, jpegBytes: ByteArray): Result<Team> {
        if (failImages) return Result.failure(IllegalStateException("offline"))
        val url = "https://cdn/${kind.filePrefix}.jpg"
        val updated = team.value!!.let { if (kind == TeamImageKind.AVATAR) it.copy(avatarUrl = url) else it.copy(coverUrl = url) }
        team.value = updated
        return Result.success(updated)
    }
    override suspend fun removeTeamImage(teamId: String, kind: TeamImageKind): Result<Team> {
        val updated = team.value!!.let { if (kind == TeamImageKind.AVATAR) it.copy(avatarUrl = null) else it.copy(coverUrl = null) }
        team.value = updated
        return Result.success(updated)
    }
    override suspend fun reorderTeams(userId: String, orderedTeamIds: List<String>): Result<Unit> = Result.success(Unit)
    override suspend fun joinTeamViaInvite(inviteCode: String, userId: String): Result<Team> =
        Result.failure(UnsupportedOperationException())
    override suspend fun generateInviteUrl(inviteCode: String): String = ""
}

@OptIn(ExperimentalCoroutinesApi::class)
class TeamDetailsViewModelTest {

    private val team = Team("t1", "Trail Blazers", "Hikes", null, null, "ABCD1234", "owner", 0, 0)

    private fun member(userId: String, role: TeamRole) =
        TeamMember("m_$userId", "t1", userId, role, userId, userId.uppercase(), null, 0)

    private fun viewModel(repo: FakeTeamRepository, scope: CoroutineScope) = TeamDetailsViewModel(
        GetTeamUseCase(repo), GetTeamMembersUseCase(repo), RefreshTeamDetailsUseCase(repo),
        UpdateTeamUseCase(repo), SetTeamImageUseCase(repo), RemoveTeamImageUseCase(repo), scope
    )

    @Test
    fun loadShowsTeamMembersAndRefreshesRemote() = runTest {
        val repo = FakeTeamRepository(team, listOf(member("owner", TeamRole.ADMIN), member("u2", TeamRole.MEMBER)))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(TeamDetailsIntent.Load("t1", "u2"))
        assertEquals("Trail Blazers", vm.uiState.value.team?.name)
        assertEquals(2, vm.uiState.value.members.size)
        assertEquals(1, repo.refreshed)
    }

    @Test
    fun ownerAndAdminsMayEditPlainMembersMayNot() = runTest {
        val members = listOf(member("owner", TeamRole.ADMIN), member("admin2", TeamRole.ADMIN), member("u3", TeamRole.MEMBER))
        for ((user, expected) in listOf("owner" to true, "admin2" to true, "u3" to false, "stranger" to false)) {
            val vm = viewModel(FakeTeamRepository(team, members), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
            vm.processIntent(TeamDetailsIntent.Load("t1", user))
            assertEquals(expected, vm.uiState.value.canEdit, "user $user")
        }
    }

    @Test
    fun creatorMayEditEvenBeforeMembersAreCached() = runTest {
        val vm = viewModel(FakeTeamRepository(team, emptyList()), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(TeamDetailsIntent.Load("t1", "owner"))
        assertTrue(vm.uiState.value.canEdit)
    }

    @Test
    fun saveReportsSuccessAndClearsDraftFlag() = runTest {
        val repo = FakeTeamRepository(team, emptyList())
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(TeamDetailsIntent.Load("t1", "owner"))
        vm.processIntent(TeamDetailsIntent.NameChanged("  New name "))
        vm.processIntent(TeamDetailsIntent.Save)

        assertEquals(TeamDetailsSuccess.TEAM_SAVED, vm.uiState.value.success)
        assertFalse(vm.uiState.value.hasUnsavedChanges)
        assertEquals("New name", repo.team.value?.name)
    }

    @Test
    fun blankNameIsRejected() = runTest {
        val repo = FakeTeamRepository(team, emptyList())
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(TeamDetailsIntent.Load("t1", "owner"))
        vm.processIntent(TeamDetailsIntent.NameChanged("   "))
        vm.processIntent(TeamDetailsIntent.Save)

        assertEquals("Team name cannot be empty", vm.uiState.value.errorMessage)
        assertEquals("Trail Blazers", repo.team.value?.name)
    }

    @Test
    fun discardRestoresStoredTeam() = runTest {
        val vm = viewModel(FakeTeamRepository(team, emptyList()), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(TeamDetailsIntent.Load("t1", "owner"))
        vm.processIntent(TeamDetailsIntent.DescriptionChanged("changed"))
        vm.processIntent(TeamDetailsIntent.DiscardChanges)

        assertEquals("Hikes", vm.uiState.value.team?.description)
        assertFalse(vm.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun imageUploadKeepsUnsavedTextEdits() = runTest {
        val vm = viewModel(FakeTeamRepository(team, emptyList()), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(TeamDetailsIntent.Load("t1", "owner"))
        vm.processIntent(TeamDetailsIntent.NameChanged("Draft"))
        vm.processIntent(TeamDetailsIntent.UploadCover(byteArrayOf(1, 2)))

        val state = vm.uiState.value
        assertEquals("https://cdn/cover.jpg", state.team?.coverUrl)
        assertEquals("Draft", state.team?.name)
        assertEquals(TeamDetailsSuccess.IMAGE_UPDATED, state.success)
    }

    @Test
    fun failedImageUploadKeepsOldImageAndShowsError() = runTest {
        val repo = FakeTeamRepository(team, emptyList()).apply { failImages = true }
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(TeamDetailsIntent.Load("t1", "owner"))
        vm.processIntent(TeamDetailsIntent.UploadAvatar(byteArrayOf(1)))

        assertEquals("offline", vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.team?.avatarUrl)
        assertFalse(vm.uiState.value.isImageUpdating)
    }

    @Test
    fun removeCoverClearsUrl() = runTest {
        val vm = viewModel(FakeTeamRepository(team.copy(coverUrl = "https://cdn/c.jpg"), emptyList()), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(TeamDetailsIntent.Load("t1", "owner"))
        vm.processIntent(TeamDetailsIntent.RemoveCover)
        assertNull(vm.uiState.value.team?.coverUrl)
    }
}
