package com.wandr.presentation.challenge

import com.wandr.domain.error.AppError
import com.wandr.domain.error.InputProblem
import com.wandr.domain.model.Challenge
import com.wandr.domain.model.ChallengeParticipant
import com.wandr.domain.model.ChallengeParticipation
import com.wandr.domain.model.ChallengeScope
import com.wandr.domain.model.ChallengeStatus
import com.wandr.domain.model.ChallengeType
import com.wandr.domain.model.LeaderboardEntry
import com.wandr.domain.model.Team
import com.wandr.domain.model.TeamImageKind
import com.wandr.domain.model.TeamMember
import com.wandr.domain.model.TeamStanding
import com.wandr.domain.repository.ChallengeRepository
import com.wandr.domain.repository.TeamRepository
import com.wandr.domain.usecase.CreateChallengeUseCase
import com.wandr.domain.usecase.EnrollTeamInChallengeUseCase
import com.wandr.domain.usecase.EvaluateChallengeStatusUseCase
import com.wandr.domain.usecase.GetChallengeParticipationsUseCase
import com.wandr.domain.usecase.GetChallengeUseCase
import com.wandr.domain.usecase.GetChallengesUseCase
import com.wandr.domain.usecase.GetMemberRankingUseCase
import com.wandr.domain.usecase.GetTeamStandingsUseCase
import com.wandr.domain.usecase.GetUserTeamsUseCase
import com.wandr.domain.usecase.JoinChallengeUseCase
import com.wandr.domain.usecase.LeaveChallengeUseCase
import com.wandr.domain.usecase.RefreshChallengesUseCase
import com.wandr.domain.usecase.RemoveChallengeCoverUseCase
import com.wandr.domain.usecase.SetChallengeCoverUseCase
import com.wandr.domain.usecase.UpdateChallengeUseCase
import com.wandr.domain.usecase.WithdrawTeamFromChallengeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeChallengeRepository(initial: List<Challenge> = emptyList()) : ChallengeRepository {
    val challenges = MutableStateFlow(initial)
    var created: Challenge? = null
    var updated: Challenge? = null
    var refreshed = 0
    var enrolled: Triple<String, String, String>? = null
    var failEnroll = false
    var standings: List<TeamStanding> = emptyList()
    var ranking: List<LeaderboardEntry> = emptyList()
    var rankingFails = false

    override fun getChallengeById(challengeId: String): Flow<Challenge?> = challenges.map { list -> list.firstOrNull { it.id == challengeId } }
    override fun getChallenges(): Flow<List<Challenge>> = challenges
    val participations = MutableStateFlow<List<ChallengeParticipation>>(emptyList())
    var left: Pair<String, String>? = null
    var withdrawn: Pair<String, String>? = null
    var failLeave = false
    override suspend fun refreshChallenges(userId: String): Result<Unit> {
        refreshed++
        return Result.success(Unit)
    }
    override fun getParticipations(userId: String): Flow<List<ChallengeParticipation>> = participations
    override suspend fun leaveChallenge(challengeId: String, userId: String): Result<Unit> {
        if (failLeave) return Result.failure(AppError.PermissionDenied())
        left = challengeId to userId
        participations.value = participations.value.filterNot { it.challengeId == challengeId }
        return Result.success(Unit)
    }
    override suspend fun withdrawTeam(challengeId: String, teamId: String): Result<Unit> {
        withdrawn = challengeId to teamId
        participations.value = participations.value.filterNot { it.challengeId == challengeId }
        return Result.success(Unit)
    }
    override fun getChallengeParticipants(challengeId: String): Flow<List<ChallengeParticipant>> = emptyFlow()
    override suspend fun getMemberRanking(challengeId: String): Result<List<LeaderboardEntry>> =
        if (rankingFails) Result.failure(IllegalStateException("offline")) else Result.success(ranking)
    override suspend fun getTeamStandings(challengeId: String): Result<List<TeamStanding>> = Result.success(standings)
    override suspend fun createChallenge(challenge: Challenge): Result<Challenge> {
        created = challenge
        challenges.value = challenges.value + challenge
        return Result.success(challenge)
    }
    override suspend fun updateChallenge(challenge: Challenge): Result<Challenge> {
        updated = challenge
        challenges.value = challenges.value.map { if (it.id == challenge.id) challenge else it }
        return Result.success(challenge)
    }
    var coverBytes: ByteArray? = null
    var coverRemoved = false
    override suspend fun setChallengeCover(challengeId: String, jpegBytes: ByteArray): Result<Challenge> {
        coverBytes = jpegBytes
        return Result.success(challenges.value.first { it.id == challengeId }.copy(coverUrl = "https://cdn/cover.jpg"))
    }
    override suspend fun removeChallengeCover(challengeId: String): Result<Challenge> {
        coverRemoved = true
        return Result.success(challenges.value.first { it.id == challengeId }.copy(coverUrl = null))
    }
    override suspend fun joinChallenge(challengeId: String, userId: String): Result<Unit> = Result.success(Unit)
    override suspend fun enrollTeam(challengeId: String, teamId: String, enrolledBy: String): Result<Unit> {
        if (failEnroll) return Result.failure(AppError.PermissionDenied())
        enrolled = Triple(challengeId, teamId, enrolledBy)
        return Result.success(Unit)
    }
}

private class FakeTeamRepository(teams: List<Team>) : TeamRepository {
    private val teamList = MutableStateFlow(teams)
    override fun getTeamById(teamId: String): Flow<Team?> = emptyFlow()
    override fun getUserTeams(userId: String): Flow<List<Team>> = teamList
    override suspend fun refreshUserTeams(userId: String): Result<Unit> = Result.success(Unit)
    override fun getTeamMembers(teamId: String): Flow<List<TeamMember>> = emptyFlow()
    override suspend fun createTeam(name: String, description: String?, creatorId: String): Result<Team> = Result.failure(UnsupportedOperationException())
    override suspend fun refreshTeamDetails(teamId: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateTeam(team: Team): Result<Team> = Result.success(team)
    override suspend fun setTeamImage(teamId: String, kind: TeamImageKind, jpegBytes: ByteArray): Result<Team> = Result.failure(UnsupportedOperationException())
    override suspend fun removeTeamImage(teamId: String, kind: TeamImageKind): Result<Team> = Result.failure(UnsupportedOperationException())
    override suspend fun reorderTeams(userId: String, orderedTeamIds: List<String>): Result<Unit> = Result.success(Unit)
    override suspend fun joinTeamViaInvite(inviteCode: String, userId: String): Result<Team> = Result.failure(UnsupportedOperationException())
    override suspend fun generateInviteUrl(inviteCode: String): String = ""
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeViewModelTest {

    private val team = Team("t1", "Trail Blazers", null, null, null, "ABCD1234", "u1", 0, 0)
    private val day = 24 * 60 * 60 * 1000L
    private val open = Challenge(
        "c1", "Open 100 km", "desc", null, "individual", "distance", 100_000.0, false,
        startDate = 0, endDate = 30 * day, createdBy = "u1", createdAt = 0, updatedAt = 0
    )

    private fun viewModel(
        repo: FakeChallengeRepository,
        teams: List<Team>,
        scope: CoroutineScope
    ) = ChallengeViewModel(
        GetChallengesUseCase(repo), GetChallengeParticipationsUseCase(repo), LeaveChallengeUseCase(repo),
        WithdrawTeamFromChallengeUseCase(repo), GetChallengeUseCase(repo), SetChallengeCoverUseCase(repo), RemoveChallengeCoverUseCase(repo),
        EvaluateChallengeStatusUseCase(), RefreshChallengesUseCase(repo), GetUserTeamsUseCase(FakeTeamRepository(teams)),
        CreateChallengeUseCase(repo), UpdateChallengeUseCase(repo),
        GetMemberRankingUseCase(repo), GetTeamStandingsUseCase(repo), EnrollTeamInChallengeUseCase(repo),
        JoinChallengeUseCase(repo), scope
    )

    @Test
    fun loadShowsChallengesTeamsAndRefreshes() = runTest {
        val repo = FakeChallengeRepository(listOf(open))
        val vm = viewModel(repo, listOf(team), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u1"))
        assertEquals(listOf(open), vm.uiState.value.challenges)
        assertEquals(listOf(team), vm.uiState.value.teams)
        assertEquals(1, repo.refreshed)
    }

    @Test
    fun statusIsDerivedAtRuntimeFromFlagAndDates() = runTest {
        val farFuture = 4_000_000_000_000L // year 2096
        val draft = open.copy(id = "draft", isActive = false)
        val planned = open.copy(id = "planned", startDate = farFuture, endDate = farFuture + day)
        val running = open.copy(id = "running", startDate = 0, endDate = farFuture)
        val over = open.copy(id = "over", startDate = 0, endDate = 1)
        val vm = viewModel(FakeChallengeRepository(listOf(draft, planned, running, over)), emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u1"))

        assertEquals(ChallengeStatus.DRAFT, vm.uiState.value.statuses["draft"])
        assertEquals(ChallengeStatus.PLANNED, vm.uiState.value.statuses["planned"])
        assertEquals(ChallengeStatus.ACTIVE, vm.uiState.value.statuses["running"])
        assertEquals(ChallengeStatus.EXPIRED, vm.uiState.value.statuses["over"])
    }

    @Test
    fun createIndividualChallengeHasNoTeam() = runTest {
        val repo = FakeChallengeRepository()
        val vm = viewModel(repo, listOf(team), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u1"))
        vm.processIntent(ChallengeIntent.StartCreate)
        vm.processIntent(ChallengeIntent.TitleChanged("  Summer hike  "))
        vm.processIntent(ChallengeIntent.SubmitForm("u1"))

        val created = assertNotNull(repo.created)
        assertEquals("Summer hike", created.title)
        assertEquals("individual", created.scope)
        assertEquals(30 * day, created.endDate - created.startDate) // default length
        assertFalse(created.isActive) // new challenges start as drafts
        assertEquals(ChallengeSuccess.CREATED, vm.uiState.value.success)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun noActivityTypeSelectedMeansAllTypesCount() = runTest {
        val repo = FakeChallengeRepository()
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.StartCreate)
        vm.processIntent(ChallengeIntent.TitleChanged("Any activity"))
        vm.processIntent(ChallengeIntent.SubmitForm("u1"))
        assertTrue(assertNotNull(repo.created).activityTypes.isEmpty())
    }

    @Test
    fun selectedActivityTypesAreToggledAndSaved() = runTest {
        val repo = FakeChallengeRepository()
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.StartCreate)
        vm.processIntent(ChallengeIntent.TitleChanged("Runners only"))
        vm.processIntent(ChallengeIntent.ActivityTypeToggled("running"))
        vm.processIntent(ChallengeIntent.ActivityTypeToggled("cycling"))
        vm.processIntent(ChallengeIntent.ActivityTypeToggled("cycling")) // off again
        assertEquals(setOf("running"), assertNotNull(vm.uiState.value.form).activityTypes)

        vm.processIntent(ChallengeIntent.SubmitForm("u1"))
        assertEquals(listOf("running"), assertNotNull(repo.created).activityTypes)
    }

    @Test
    fun editingStartsFromTheSavedActivityTypes() = runTest {
        val repo = FakeChallengeRepository()
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.StartCreate)
        vm.processIntent(ChallengeIntent.TitleChanged("Hikes"))
        vm.processIntent(ChallengeIntent.ActivityTypeToggled("hiking"))
        vm.processIntent(ChallengeIntent.SubmitForm("u1"))

        vm.processIntent(ChallengeIntent.LoadChallenges("u1"))
        val saved = assertNotNull(repo.created)
        vm.processIntent(ChallengeIntent.StartEdit(saved.id))
        assertEquals(setOf("hiking"), vm.uiState.value.form?.activityTypes)
    }

    @Test
    fun activeToggleDecidesBetweenDraftAndActive() = runTest {
        val repo = FakeChallengeRepository()
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.StartCreate)
        assertFalse(assertNotNull(vm.uiState.value.form).isActive)

        vm.processIntent(ChallengeIntent.TitleChanged("Live"))
        vm.processIntent(ChallengeIntent.ActiveChanged(true))
        vm.processIntent(ChallengeIntent.SubmitForm("u1"))

        assertTrue(assertNotNull(repo.created).isActive)
    }

    @Test
    fun challengesCanBePlannedInTheFuture() = runTest {
        val repo = FakeChallengeRepository()
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.StartCreate)
        vm.processIntent(ChallengeIntent.TitleChanged("Next summer"))
        vm.processIntent(ChallengeIntent.StartDateChanged(4_000_000_000_000L))
        vm.processIntent(ChallengeIntent.EndDateChanged(4_000_000_000_000L + 7 * day))
        vm.processIntent(ChallengeIntent.SubmitForm("u1"))

        val created = assertNotNull(repo.created)
        assertEquals(4_000_000_000_000L, created.startDate)
        assertEquals(4_000_000_000_000L + 7 * day, created.endDate)
    }

    @Test
    fun movingTheStartPastTheEndMovesTheEndAlongKeepingTheLength() = runTest {
        val vm = viewModel(FakeChallengeRepository(), emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.StartCreate)
        val before = assertNotNull(vm.uiState.value.form)
        val length = before.endDate - before.startDate

        vm.processIntent(ChallengeIntent.StartDateChanged(before.endDate + 5 * day))

        val form = assertNotNull(vm.uiState.value.form)
        assertEquals(before.endDate + 5 * day, form.startDate)
        assertEquals(length, form.endDate - form.startDate)
    }

    @Test
    fun endBeforeStartIsRejected() = runTest {
        val repo = FakeChallengeRepository()
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.StartCreate)
        vm.processIntent(ChallengeIntent.TitleChanged("Backwards"))
        val start = assertNotNull(vm.uiState.value.form).startDate
        vm.processIntent(ChallengeIntent.EndDateChanged(start - day))
        vm.processIntent(ChallengeIntent.SubmitForm("u1"))

        assertEquals(
            InputProblem.END_BEFORE_START,
            assertIs<AppError.InvalidInput>(vm.uiState.value.error).problem
        )
        assertNull(repo.created)
    }

    @Test
    fun groupChallengeIsCreatedWithoutATeamBecauseTeamsEnrollLater() = runTest {
        val repo = FakeChallengeRepository()
        val vm = viewModel(repo, listOf(team), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u1"))
        vm.processIntent(ChallengeIntent.StartCreate)
        vm.processIntent(ChallengeIntent.TitleChanged("Team sprint"))
        vm.processIntent(ChallengeIntent.ScopeChanged(ChallengeScope.GROUP))
        vm.processIntent(ChallengeIntent.RequireAllMembersCompletionChanged(true))
        vm.processIntent(ChallengeIntent.SubmitForm("u1"))

        val created = assertNotNull(repo.created)
        assertEquals("group", created.scope)
        assertTrue(created.requireAllMembersCompletion)
        assertEquals(ChallengeSuccess.CREATED, vm.uiState.value.success)
    }

    @Test
    fun switchingBackToIndividualDropsRequireAll() = runTest {
        val vm = viewModel(FakeChallengeRepository(), listOf(team), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.StartCreate)
        vm.processIntent(ChallengeIntent.ScopeChanged(ChallengeScope.GROUP))
        vm.processIntent(ChallengeIntent.RequireAllMembersCompletionChanged(true))
        vm.processIntent(ChallengeIntent.ScopeChanged(ChallengeScope.INDIVIDUAL))

        assertFalse(assertNotNull(vm.uiState.value.form).requireAllMembersCompletion)
    }

    @Test
    fun enrollTeamReportsSuccessAndPassesAllIds() = runTest {
        val repo = FakeChallengeRepository()
        val vm = viewModel(repo, listOf(team), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.EnrollTeam("c2", "t1", "u1"))

        assertEquals(Triple("c2", "t1", "u1"), repo.enrolled)
        assertEquals(ChallengeSuccess.TEAM_ENROLLED, vm.uiState.value.success)
    }

    @Test
    fun enrollTeamFailureIsShownAsError() = runTest {
        val repo = FakeChallengeRepository().apply { failEnroll = true }
        val vm = viewModel(repo, listOf(team), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.EnrollTeam("c2", "t1", "u3"))

        assertIs<AppError.PermissionDenied>(vm.uiState.value.error)
        assertNull(vm.uiState.value.success)
    }

    @Test
    fun selectChallengeLoadsTeamStandingsAndTheMemberRanking() = runTest {
        val repo = FakeChallengeRepository().apply {
            standings = listOf(
                TeamStanding(1, "t2", "City Runners", null, 80_000.0, 80.0, 4, 1, false),
                TeamStanding(2, "t1", "Trail Blazers", null, 50_000.0, 50.0, 3, 0, false)
            )
            ranking = listOf(LeaderboardEntry(1, "u1", "A", null, 30_000.0, 30.0, false))
        }
        val vm = viewModel(repo, listOf(team), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.SelectChallenge("c2"))

        assertEquals(listOf("t2", "t1"), vm.uiState.value.standings.map { it.teamId })
        assertEquals(1, vm.uiState.value.leaderboard.size)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun anUnavailableRankingLeavesTheListEmptyWithoutAnError() = runTest {
        val repo = FakeChallengeRepository().apply { rankingFails = true }
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.SelectChallenge("c2"))

        assertTrue(vm.uiState.value.leaderboard.isEmpty())
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun blankTitleAndNonPositiveTargetAreRejected() = runTest {
        val repo = FakeChallengeRepository()
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.StartCreate)
        vm.processIntent(ChallengeIntent.SubmitForm("u1"))
        assertEquals(
            InputProblem.TITLE_REQUIRED,
            assertIs<AppError.InvalidInput>(vm.uiState.value.error).problem
        )

        vm.processIntent(ChallengeIntent.TitleChanged("Ok"))
        vm.processIntent(ChallengeIntent.TargetValueChanged(0.0))
        vm.processIntent(ChallengeIntent.SubmitForm("u1"))
        assertEquals(
            InputProblem.TARGET_NOT_POSITIVE,
            assertIs<AppError.InvalidInput>(vm.uiState.value.error).problem
        )
        assertNull(repo.created)
    }

    @Test
    fun startEditFillsFormAndSubmitUpdatesInsteadOfCreating() = runTest {
        val repo = FakeChallengeRepository(listOf(open))
        val vm = viewModel(repo, listOf(team), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u1"))
        vm.processIntent(ChallengeIntent.StartEdit("c1"))

        val form = assertNotNull(vm.uiState.value.form)
        assertEquals("Open 100 km", form.title)
        assertEquals(ChallengeType.DISTANCE, form.type)
        assertEquals(0L, form.startDate)
        assertEquals(30 * day, form.endDate)
        assertTrue(form.isActive)
        assertTrue(form.isEditing)

        vm.processIntent(ChallengeIntent.TitleChanged("Open 150 km"))
        vm.processIntent(ChallengeIntent.TargetValueChanged(150_000.0))
        vm.processIntent(ChallengeIntent.SubmitForm("u1"))

        assertNull(repo.created)
        val updated = assertNotNull(repo.updated)
        assertEquals("c1", updated.id)
        assertEquals("Open 150 km", updated.title)
        assertEquals(150_000.0, updated.targetValue)
        assertEquals(0L, updated.startDate) // editing keeps the original start
        assertEquals(ChallengeSuccess.UPDATED, vm.uiState.value.success)
    }

    @Test
    fun changingTypeResetsTargetToSensibleDefault() = runTest {
        val vm = viewModel(FakeChallengeRepository(), emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.StartCreate)
        vm.processIntent(ChallengeIntent.TypeChanged(ChallengeType.ELEVATION))
        assertEquals(5_000.0, assertNotNull(vm.uiState.value.form).targetValue)
    }

    @Test
    fun discardClosesFormAndClearMessagesResetsSuccess() = runTest {
        val repo = FakeChallengeRepository()
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.StartCreate)
        vm.processIntent(ChallengeIntent.TitleChanged("X"))
        vm.processIntent(ChallengeIntent.SubmitForm("u1"))
        assertEquals(ChallengeSuccess.CREATED, vm.uiState.value.success)

        vm.processIntent(ChallengeIntent.ClearMessages)
        vm.processIntent(ChallengeIntent.DiscardForm)
        assertNull(vm.uiState.value.success)
        assertNull(vm.uiState.value.form)
    }

    @Test
    fun coverUploadAndRemovalApplyToTheSelectedChallenge() = runTest {
        val repo = FakeChallengeRepository(listOf(open))
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u1")) // the detail screen always loads the user first
        vm.processIntent(ChallengeIntent.SelectChallenge("c1"))
        assertEquals("c1", vm.uiState.value.selectedChallenge?.id)

        vm.processIntent(ChallengeIntent.UploadCover(byteArrayOf(1, 2, 3)))
        assertEquals(3, repo.coverBytes?.size)
        assertEquals(ChallengeSuccess.IMAGE_UPDATED, vm.uiState.value.success)
        assertFalse(vm.uiState.value.isImageUpdating)

        vm.processIntent(ChallengeIntent.RemoveCover)
        assertTrue(repo.coverRemoved)
    }

    @Test
    fun ownerCanEditFromTheSelectedChallenge() = runTest {
        val repo = FakeChallengeRepository(listOf(open))
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u1"))
        vm.processIntent(ChallengeIntent.SelectChallenge("c1"))
        vm.processIntent(ChallengeIntent.StartEdit("c1"))

        assertEquals("c1", assertNotNull(vm.uiState.value.form).challengeId)
    }

    @Test
    fun participationsAreExposedByChallengeId() = runTest {
        val repo = FakeChallengeRepository(listOf(open))
        repo.participations.value = listOf(ChallengeParticipation("c1", null))
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u1"))

        assertEquals(ChallengeParticipation("c1", null), vm.uiState.value.participations["c1"])
    }

    @Test
    fun leavingRemovesTheParticipationAndReportsSuccess() = runTest {
        val repo = FakeChallengeRepository(listOf(open))
        repo.participations.value = listOf(ChallengeParticipation("c1", null))
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u1"))
        vm.processIntent(ChallengeIntent.LeaveChallenge("c1", "u1"))

        assertEquals("c1" to "u1", repo.left)
        assertEquals(ChallengeSuccess.LEFT, vm.uiState.value.success)
        assertTrue(vm.uiState.value.participations.isEmpty())
    }

    @Test
    fun leaveFailureIsShownAsError() = runTest {
        val repo = FakeChallengeRepository(listOf(open)).apply { failLeave = true }
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LeaveChallenge("c1", "u1"))

        assertIs<AppError.PermissionDenied>(vm.uiState.value.error)
        assertNull(vm.uiState.value.success)
    }

    @Test
    fun withdrawingATeamReportsSuccess() = runTest {
        val repo = FakeChallengeRepository()
        repo.participations.value = listOf(ChallengeParticipation("c2", "t1"))
        val vm = viewModel(repo, listOf(team), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u1"))
        vm.processIntent(ChallengeIntent.WithdrawTeam("c2", "t1"))

        assertEquals("c2" to "t1", repo.withdrawn)
        assertEquals(ChallengeSuccess.TEAM_WITHDRAWN, vm.uiState.value.success)
        assertTrue(vm.uiState.value.participations.isEmpty())
    }

    @Test
    fun enrollingRefreshesSoTheNewParticipationShowsUp() = runTest {
        val repo = FakeChallengeRepository()
        val vm = viewModel(repo, listOf(team), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u1"))
        val refreshesBefore = repo.refreshed
        vm.processIntent(ChallengeIntent.EnrollTeam("c2", "t1", "u1"))

        assertEquals(refreshesBefore + 1, repo.refreshed)
    }

    @Test
    fun onlyTheCreatorMayEditTheSelectedChallenge() = runTest {
        // `open` was created by u1.
        for ((user, expected) in listOf("u1" to true, "u2" to false)) {
            val vm = viewModel(FakeChallengeRepository(listOf(open)), emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
            vm.processIntent(ChallengeIntent.LoadChallenges(user))
            vm.processIntent(ChallengeIntent.SelectChallenge("c1"))
            assertEquals(expected, vm.uiState.value.canEdit, "user $user")
        }
    }

    @Test
    fun nonOwnersCannotOpenTheEditForm() = runTest {
        val vm = viewModel(FakeChallengeRepository(listOf(open)), emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u2"))
        vm.processIntent(ChallengeIntent.StartEdit("c1"))

        assertNull(vm.uiState.value.form)
        assertIs<AppError.PermissionDenied>(vm.uiState.value.error)
    }

    @Test
    fun nonOwnersCannotChangeTheCover() = runTest {
        val repo = FakeChallengeRepository(listOf(open))
        val vm = viewModel(repo, emptyList(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ChallengeIntent.LoadChallenges("u2"))
        vm.processIntent(ChallengeIntent.SelectChallenge("c1"))
        vm.processIntent(ChallengeIntent.UploadCover(byteArrayOf(1)))
        vm.processIntent(ChallengeIntent.RemoveCover)

        assertNull(repo.coverBytes)
        assertFalse(repo.coverRemoved)
        assertIs<AppError.PermissionDenied>(vm.uiState.value.error)
    }
}
