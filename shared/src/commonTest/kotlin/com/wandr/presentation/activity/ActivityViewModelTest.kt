package com.wandr.presentation.activity

import com.wandr.domain.model.Activity
import com.wandr.domain.model.ConflictResolution
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.repository.ActivityFeedRepository
import com.wandr.domain.repository.ActivityRepository
import com.wandr.domain.usecase.CreateManualActivityUseCase
import com.wandr.domain.usecase.DeleteActivityUseCase
import com.wandr.domain.usecase.GetActivityTrackUseCase
import com.wandr.domain.usecase.GetActivityUseCase
import com.wandr.domain.usecase.GetTeamActivitiesUseCase
import com.wandr.domain.usecase.GetUserActivitiesUseCase
import com.wandr.domain.usecase.RecordGpsActivityUseCase
import com.wandr.domain.usecase.RefreshActivitiesUseCase
import com.wandr.domain.usecase.UpdateActivityUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

private class FakeActivityRepository(initial: List<Activity> = emptyList()) : ActivityRepository {
    val activities = MutableStateFlow(initial)
    var saved: Activity? = null
    var savedTrackpoints: List<GpsTrackpoint>? = null
    var track: List<GpsTrackpoint> = emptyList()
    var deleted: String? = null

    override fun getUserActivities(userId: String): Flow<List<Activity>> = activities.map { l -> l.filter { it.userId == userId } }
    override fun getActivityById(id: String): Flow<Activity?> = activities.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun getTrackpoints(activityId: String): List<GpsTrackpoint> = track
    override fun getUserActivityCount(userId: String): Flow<Int> = activities.map { it.size }
    override fun getTeamActivities(teamId: String): Flow<List<Activity>> = activities
    override suspend fun getOverlappingActivities(userId: String, startTime: Long, endTime: Long): List<Activity> =
        activities.value.filter { it.userId == userId && it.startTime < endTime && it.endTime > startTime }
    override suspend fun saveActivity(activity: Activity, trackpoints: List<GpsTrackpoint>?): Result<Activity> {
        saved = activity
        savedTrackpoints = trackpoints
        activities.value = activities.value.filterNot { it.id == activity.id } + activity
        return Result.success(activity)
    }
    override suspend fun deleteActivity(id: String): Result<Unit> {
        deleted = id
        activities.value = activities.value.filterNot { it.id == id }
        return Result.success(Unit)
    }
}

private class CountingSocialRepository : com.wandr.domain.repository.SocialRepository {
    var countCalls = 0
    var failLike = false
    val likes = mutableListOf<Pair<String, Boolean>>()
    var counts: Map<String, com.wandr.domain.model.SocialCounts> = emptyMap()

    override suspend fun getCounts(type: com.wandr.domain.model.SocialEntityType, entityIds: List<String>): Result<Map<String, com.wandr.domain.model.SocialCounts>> {
        countCalls++
        return Result.success(entityIds.associateWith { counts[it] ?: com.wandr.domain.model.SocialCounts() })
    }
    override suspend fun setLike(type: com.wandr.domain.model.SocialEntityType, entityId: String, userId: String, liked: Boolean): Result<Unit> {
        if (failLike) return Result.failure(IllegalStateException("offline"))
        likes += entityId to liked
        return Result.success(Unit)
    }
    override suspend fun getSummary(type: com.wandr.domain.model.SocialEntityType, entityId: String, userId: String) = Result.failure<com.wandr.domain.model.SocialSummary>(UnsupportedOperationException())
    override suspend fun getComments(type: com.wandr.domain.model.SocialEntityType, entityId: String, userId: String) = Result.failure<List<com.wandr.domain.model.Comment>>(UnsupportedOperationException())
    override suspend fun addComment(type: com.wandr.domain.model.SocialEntityType, entityId: String, userId: String, content: String) = Result.failure<com.wandr.domain.model.Comment>(UnsupportedOperationException())
    override suspend fun updateComment(commentId: String, content: String) = Result.failure<com.wandr.domain.model.Comment>(UnsupportedOperationException())
    override suspend fun deleteComment(commentId: String) = Result.failure<Unit>(UnsupportedOperationException())
    override suspend fun setReaction(commentId: String, userId: String, emoji: String, reacted: Boolean) = Result.failure<Unit>(UnsupportedOperationException())
}

private class FakeAuthors(val profiles: List<com.wandr.domain.model.Profile> = emptyList()) : com.wandr.domain.repository.ProfileRepository {
    override fun getProfile(userId: String): Flow<com.wandr.domain.model.Profile?> = MutableStateFlow(null)
    override fun getProfiles(userIds: List<String>): Flow<List<com.wandr.domain.model.Profile>> = MutableStateFlow(profiles.filter { it.id in userIds })
    override suspend fun refreshProfile(userId: String) = Result.success(Unit)
    override suspend fun updateProfile(profile: com.wandr.domain.model.Profile) = Result.success(profile)
    override suspend fun setAvatar(userId: String, jpegBytes: ByteArray) = Result.failure<com.wandr.domain.model.Profile>(UnsupportedOperationException())
    override suspend fun removeAvatar(userId: String) = Result.failure<com.wandr.domain.model.Profile>(UnsupportedOperationException())
}

private object System0 { fun now() = kotlin.time.Clock.System.now().toEpochMilliseconds() }

private object NoFeed : ActivityFeedRepository {
    override suspend fun refreshTeam(teamId: String, userId: String) = Result.success(Unit)
    override suspend fun refreshUser(userId: String) = Result.success(Unit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {

    private val manual = Activity(
        id = "a1", userId = "u1", teamId = null, title = "Walk", description = "desc", activityType = "hiking",
        distanceMeters = 5000.0, durationSeconds = 3600.0, elevationGainMeters = 100.0, fitFilePath = null,
        startTime = 1_000_000L, endTime = 1_000_000L + 3_600_000L, isManualEntry = true, createdAt = 0L, updatedAt = 0L
    )
    private val recorded = manual.copy(
        id = "a2", title = "Run", activityType = "running", isManualEntry = false,
        fitFilePath = "/files/fit/a2.fit", distanceMeters = 10_000.0, durationSeconds = 3000.0
    )

    private fun viewModel(
        repo: FakeActivityRepository,
        scope: CoroutineScope,
        social: CountingSocialRepository = CountingSocialRepository(),
        authors: FakeAuthors = FakeAuthors(),
        clock: () -> Long = { System0.now() }
    ) = ActivityViewModel(
        GetUserActivitiesUseCase(repo), GetTeamActivitiesUseCase(repo), GetActivityUseCase(repo),
        GetActivityTrackUseCase(repo), CreateManualActivityUseCase(repo), UpdateActivityUseCase(repo),
        RecordGpsActivityUseCase(repo), DeleteActivityUseCase(repo), RefreshActivitiesUseCase(NoFeed),
        com.wandr.domain.usecase.GetSocialCountsUseCase(social), com.wandr.domain.usecase.SetLikeUseCase(social),
        com.wandr.domain.usecase.GetProfilesUseCase(authors), scope, clock
    )

    @Test
    fun createManualActivityFromTheForm() = runTest {
        val repo = FakeActivityRepository()
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        vm.processIntent(ActivityIntent.StartCreate)
        vm.processIntent(ActivityIntent.TitleChanged("Morning hike"))
        vm.processIntent(ActivityIntent.TypeChanged("running"))
        vm.processIntent(ActivityIntent.DistanceChanged(12.5))
        vm.processIntent(ActivityIntent.DurationChanged(90.0))
        vm.processIntent(ActivityIntent.ElevationChanged(300.0))
        vm.processIntent(ActivityIntent.SubmitForm("u1", "t1"))

        val saved = assertNotNull(repo.saved)
        assertEquals("Morning hike", saved.title)
        assertEquals("running", saved.activityType)
        assertEquals(12_500.0, saved.distanceMeters)
        assertEquals(5400.0, saved.durationSeconds)
        assertEquals(300.0, saved.elevationGainMeters)
        assertEquals("t1", saved.teamId)
        assertTrue(saved.isManualEntry)
        assertEquals(ActivitySuccess.CREATED, vm.uiState().success)
        assertFalse(vm.uiState().isSaving)
    }

    @Test
    fun blankTitleIsRejectedAndNothingIsSaved() = runTest {
        val repo = FakeActivityRepository()
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.StartCreate)
        vm.processIntent(ActivityIntent.SubmitForm("u1", null))

        assertEquals(
            com.wandr.domain.error.InputProblem.TITLE_REQUIRED,
            assertIs<com.wandr.domain.error.AppError.InvalidInput>(vm.uiState().error).problem
        )
        assertNull(repo.saved)
    }

    @Test
    fun newActivityStartsDuringThePastDurationByDefault() = runTest {
        val vm = viewModel(FakeActivityRepository(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.StartCreate)
        val form = assertNotNull(vm.uiState().form)
        assertTrue(form.startTime > 0)
        assertFalse(form.isEditing)
    }

    @Test
    fun ownerEditsAManualActivity() = runTest {
        val repo = FakeActivityRepository(listOf(manual))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        vm.processIntent(ActivityIntent.StartEdit("a1"))

        val form = assertNotNull(vm.uiState().form)
        assertEquals("Walk", form.title)
        assertEquals(5.0, form.distanceKm)
        assertEquals(60.0, form.durationMinutes)
        assertFalse(form.isMeasured)

        vm.processIntent(ActivityIntent.TitleChanged("Long walk"))
        vm.processIntent(ActivityIntent.DistanceChanged(8.0))
        vm.processIntent(ActivityIntent.DurationChanged(120.0))
        vm.processIntent(ActivityIntent.SubmitForm("u1", null))

        val saved = assertNotNull(repo.saved)
        assertEquals("a1", saved.id)
        assertEquals("Long walk", saved.title)
        assertEquals(8000.0, saved.distanceMeters)
        assertEquals(7200.0, saved.durationSeconds)
        assertEquals(saved.startTime + 7_200_000L, saved.endTime)
        assertEquals(ActivitySuccess.UPDATED, vm.uiState().success)
    }

    @Test
    fun measuredValuesOfARecordedActivityStayUntouched() = runTest {
        val repo = FakeActivityRepository(listOf(recorded))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        vm.processIntent(ActivityIntent.StartEdit("a2"))
        assertTrue(assertNotNull(vm.uiState().form).isMeasured)

        vm.processIntent(ActivityIntent.TitleChanged("Evening run"))
        vm.processIntent(ActivityIntent.DistanceChanged(1.0)) // ignored: measured
        vm.processIntent(ActivityIntent.SubmitForm("u1", null))

        val saved = assertNotNull(repo.saved)
        assertEquals("Evening run", saved.title)
        assertEquals(10_000.0, saved.distanceMeters)
        assertEquals(3000.0, saved.durationSeconds)
        assertEquals("/files/fit/a2.fit", saved.fitFilePath) // the local track stays linked
        assertFalse(saved.isManualEntry)
    }

    @Test
    fun onlyTheOwnerCanEdit() = runTest {
        val repo = FakeActivityRepository(listOf(manual))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u2")) // someone else
        vm.processIntent(ActivityIntent.SelectActivity("a1"))
        assertFalse(vm.uiState().canEdit)

        vm.processIntent(ActivityIntent.StartEdit("a1"))

        assertNull(vm.uiState().form)
        assertIs<com.wandr.domain.error.AppError.PermissionDenied>(vm.uiState().error)
        assertNull(repo.saved)
    }

    @Test
    fun ownerOfTheSelectedActivityMayEdit() = runTest {
        val vm = viewModel(FakeActivityRepository(listOf(manual)), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        vm.processIntent(ActivityIntent.SelectActivity("a1"))

        assertEquals("a1", vm.uiState().selectedActivity?.id)
        assertTrue(vm.uiState().canEdit)
    }

    @Test
    fun selectingARecordedActivityLoadsItsLocalTrack() = runTest {
        val repo = FakeActivityRepository(listOf(recorded)).apply {
            track = listOf(GpsTrackpoint(47.0, 11.0, 1000.0, 0L, 1.5f))
        }
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        vm.processIntent(ActivityIntent.SelectActivity("a2"))

        assertEquals(1, vm.uiState().selectedTrack.size)
    }

    @Test
    fun manualEntriesHaveNoTrack() = runTest {
        val vm = viewModel(FakeActivityRepository(listOf(manual)), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        vm.processIntent(ActivityIntent.SelectActivity("a1"))
        assertTrue(vm.uiState().selectedTrack.isEmpty())
    }

    @Test
    fun recordingStoresTheTrackpointsAndReportsSuccess() = runTest {
        val repo = FakeActivityRepository()
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        vm.processIntent(ActivityIntent.StartGpsTracking("cycling"))
        vm.processIntent(ActivityIntent.AddTrackpoint(GpsTrackpoint(47.0, 11.0, 1000.0, 1000L, 5f)))
        vm.processIntent(ActivityIntent.AddTrackpoint(GpsTrackpoint(47.001, 11.001, 1005.0, 6000L, 5f)))
        vm.processIntent(ActivityIntent.StopAndSaveGpsTracking("u1", null, ""))

        val saved = assertNotNull(repo.saved)
        assertEquals("cycling", saved.activityType)
        assertEquals("Cycling Workout", saved.title)
        assertEquals(2, repo.savedTrackpoints?.size)
        assertEquals(ActivitySuccess.RECORDED, vm.uiState().success)
        assertFalse(vm.uiState().isTracking)
    }

    @Test
    fun discardClosesTheFormAndClearMessagesResetsSuccess() = runTest {
        val vm = viewModel(FakeActivityRepository(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.StartCreate)
        vm.processIntent(ActivityIntent.TitleChanged("X"))
        vm.processIntent(ActivityIntent.SubmitForm("u1", null))
        assertEquals(ActivitySuccess.CREATED, vm.uiState().success)

        vm.processIntent(ActivityIntent.ClearMessages)
        vm.processIntent(ActivityIntent.DiscardForm)
        assertNull(vm.uiState().success)
        assertNull(vm.uiState().form)
    }

    private fun ActivityViewModel.uiState() = state.value

    // --- Time conflicts -------------------------------------------------------------------------------------

    /** Creates a manual activity that overlaps [manual] (1_000_000 .. 4_600_000). */
    private fun ActivityViewModel.submitOverlapping(title: String = "Overlap") {
        processIntent(ActivityIntent.StartCreate)
        processIntent(ActivityIntent.TitleChanged(title))
        processIntent(ActivityIntent.StartTimeChanged(manual.startTime + 1_800_000L))
        processIntent(ActivityIntent.DurationChanged(60.0))
        processIntent(ActivityIntent.SubmitForm("u1", null))
    }

    @Test
    fun overlapOpensTheWizardInsteadOfSaving() = runTest {
        val repo = FakeActivityRepository(listOf(manual))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.submitOverlapping()

        val conflict = assertNotNull(vm.uiState().conflict)
        assertEquals(listOf("a1"), conflict.conflicting.map { it.id })
        assertTrue(conflict.dismissible)
        assertNull(repo.saved)
        assertNull(vm.uiState().error)
        assertFalse(vm.uiState().isSaving)
    }

    @Test
    fun mergeSavesOneActivityAndRemovesTheOverlapping() = runTest {
        val repo = FakeActivityRepository(listOf(manual))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.submitOverlapping()
        vm.processIntent(ActivityIntent.ResolveConflict(ConflictResolution.MERGE))

        assertEquals(ActivitySuccess.MERGED, vm.uiState().success)
        assertNull(vm.uiState().conflict)
        assertEquals(1, repo.activities.value.size)
        assertEquals("a1", repo.deleted)
        assertEquals(manual.startTime, repo.saved?.startTime)
    }

    @Test
    fun trimSavesOnlyTheFreePart() = runTest {
        val repo = FakeActivityRepository(listOf(manual))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.submitOverlapping()
        vm.processIntent(ActivityIntent.ResolveConflict(ConflictResolution.TRIM))

        assertEquals(ActivitySuccess.TRIMMED, vm.uiState().success)
        assertEquals(manual.endTime, repo.saved?.startTime)
        assertEquals(2, repo.activities.value.size)
    }

    @Test
    fun discardKeepsTheExistingActivities() = runTest {
        val repo = FakeActivityRepository(listOf(manual))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.submitOverlapping()
        vm.processIntent(ActivityIntent.ResolveConflict(ConflictResolution.DISCARD))

        assertEquals(ActivitySuccess.DISCARDED, vm.uiState().success)
        assertNull(vm.uiState().conflict)
        assertNull(repo.saved)
        assertEquals(listOf("a1"), repo.activities.value.map { it.id })
    }

    @Test
    fun dismissingReturnsToTheForm() = runTest {
        val repo = FakeActivityRepository(listOf(manual))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.submitOverlapping()
        vm.processIntent(ActivityIntent.DismissConflict)

        assertNull(vm.uiState().conflict)
        assertNotNull(vm.uiState().form)
        assertNull(vm.uiState().success)
    }

    @Test
    fun editingWithoutChangingTheTimeIgnoresExistingOverlaps() = runTest {
        val overlapping = manual.copy(id = "a3", startTime = manual.startTime + 60_000L, endTime = manual.endTime + 60_000L)
        val repo = FakeActivityRepository(listOf(manual, overlapping))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        vm.processIntent(ActivityIntent.StartEdit("a1"))
        vm.processIntent(ActivityIntent.TitleChanged("Renamed"))
        vm.processIntent(ActivityIntent.SubmitForm("u1", null))

        assertNull(vm.uiState().conflict)
        assertEquals(ActivitySuccess.UPDATED, vm.uiState().success)
        assertEquals("Renamed", repo.saved?.title)
    }

    @Test
    fun recordingConflictCannotBeDismissedAndDiscardDropsTheRecording() = runTest {
        val repo = FakeActivityRepository(listOf(manual))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        vm.processIntent(ActivityIntent.StartGpsTracking("running"))
        // The recording started "now", i.e. long after the stored activity: force an overlap with a running one.
        repo.activities.value = listOf(manual.copy(startTime = 0L, endTime = Long.MAX_VALUE / 2))
        vm.processIntent(ActivityIntent.AddTrackpoint(GpsTrackpoint(1.0, 1.0, 0.0, 0L)))
        vm.processIntent(ActivityIntent.StopAndSaveGpsTracking("u1", null, "Run"))

        val conflict = assertNotNull(vm.uiState().conflict)
        assertFalse(conflict.dismissible)
        vm.processIntent(ActivityIntent.DismissConflict)
        assertNotNull(vm.uiState().conflict)
        assertEquals(1, vm.uiState().liveTrackpoints.size) // nothing lost yet

        vm.processIntent(ActivityIntent.ResolveConflict(ConflictResolution.DISCARD))
        assertEquals(ActivitySuccess.DISCARDED, vm.uiState().success)
        assertTrue(vm.uiState().liveTrackpoints.isEmpty())
        assertNull(repo.saved)
    }

    // --- Feed cards: counts, authors, privacy -----------------------------------------------------------------

    @Test
    fun likesAndCommentsOfTheWholeListAreFetchedInOneCall() = runTest {
        val social = CountingSocialRepository().apply {
            counts = mapOf("a1" to com.wandr.domain.model.SocialCounts(3, 2, true))
        }
        val repo = FakeActivityRepository(listOf(manual, recorded))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)), social)
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))

        assertEquals(1, social.countCalls) // one call for both cards, not two per card
        assertEquals(3, vm.uiState().socialCounts.getValue("a1").likeCount)
        assertEquals(0, vm.uiState().socialCounts.getValue("a2").commentCount)
    }

    @Test
    fun anEditedItemDoesNotTriggerNewCountRequests() = runTest {
        val social = CountingSocialRepository()
        val repo = FakeActivityRepository(listOf(manual))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)), social)
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        repo.activities.value = listOf(manual.copy(title = "Renamed"))
        assertEquals(1, social.countCalls)

        repo.activities.value = listOf(manual, recorded) // a new card appears
        assertEquals(2, social.countCalls)
    }

    @Test
    fun authorsOfTheCardsAreProvided() = runTest {
        val author = com.wandr.domain.model.Profile(
            id = "u1", username = "alex", displayName = "Alex", avatarUrl = null, bio = null, createdAt = 0L, updatedAt = 0L
        )
        val vm = viewModel(FakeActivityRepository(listOf(manual)), CoroutineScope(UnconfinedTestDispatcher(testScheduler)), authors = FakeAuthors(listOf(author)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        assertEquals("Alex", vm.uiState().authors["u1"]?.displayName)
    }

    @Test
    fun likeFromTheCardIsOptimisticAndRevertedOnFailure() = runTest {
        val social = CountingSocialRepository().apply { counts = mapOf("a1" to com.wandr.domain.model.SocialCounts(2, 0, false)) }
        val vm = viewModel(FakeActivityRepository(listOf(manual)), CoroutineScope(UnconfinedTestDispatcher(testScheduler)), social)
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))

        vm.processIntent(ActivityIntent.ToggleActivityLike("a1"))
        assertEquals(com.wandr.domain.model.SocialCounts(3, 0, true), vm.uiState().socialCounts["a1"])
        assertEquals(listOf("a1" to true), social.likes)

        social.failLike = true
        vm.processIntent(ActivityIntent.ToggleActivityLike("a1")) // un-like fails
        assertEquals(com.wandr.domain.model.SocialCounts(3, 0, true), vm.uiState().socialCounts["a1"])
        assertIs<com.wandr.domain.error.AppError.Unknown>(vm.uiState().error)
    }

    @Test
    fun ownerCanHideTheMapOfARecordedActivity() = runTest {
        val withRoute = recorded.copy(polyline = "_p~iF~ps|U_ulLnnqC")
        val repo = FakeActivityRepository(listOf(withRoute))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        vm.processIntent(ActivityIntent.StartEdit("a2"))

        val form = assertNotNull(vm.uiState().form)
        assertTrue(form.hasRoute)
        assertTrue(form.showMap)

        vm.processIntent(ActivityIntent.ShowMapChanged(false))
        vm.processIntent(ActivityIntent.SubmitForm("u1", null))
        assertFalse(assertNotNull(repo.saved).showMap)
        assertEquals(withRoute.polyline, repo.saved?.polyline) // only the visibility changes
    }

    @Test
    fun manualActivityHasNoRouteSoThePrivacySwitchIsNotOffered() = runTest {
        val vm = viewModel(FakeActivityRepository(listOf(manual)), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.LoadUserActivities("u1"))
        vm.processIntent(ActivityIntent.StartEdit("a1"))
        assertFalse(assertNotNull(vm.uiState().form).hasRoute)
    }

    // --- Recording ----------------------------------------------------------------------------------------

    /** About 111 m north of the previous latitude step. */
    private fun fix(index: Int, altitude: Double = 400.0) = GpsTrackpoint(47.0 + index * 0.001, 8.0, altitude, index * 3000L)

    @Test
    fun theClockCountsSecondsAndSkipsPausedTime() = runTest {
        var now = 1_000_000L
        val vm = viewModel(FakeActivityRepository(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)), clock = { now })
        vm.processIntent(ActivityIntent.StartGpsTracking("running"))

        now += 3_000; vm.processIntent(ActivityIntent.Tick(now))
        assertEquals(3.0, vm.uiState().liveDurationSeconds)

        vm.processIntent(ActivityIntent.PauseGpsTracking)
        now += 60_000; vm.processIntent(ActivityIntent.Tick(now))
        assertEquals(3.0, vm.uiState().liveDurationSeconds) // a minute of pause does not count

        vm.processIntent(ActivityIntent.ResumeGpsTracking)
        now += 2_000; vm.processIntent(ActivityIntent.Tick(now))
        assertEquals(5.0, vm.uiState().liveDurationSeconds)
    }

    @Test
    fun distanceSplitsAndPaceFromFixes() = runTest {
        var now = 0L
        val vm = viewModel(FakeActivityRepository(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)), clock = { now })
        vm.processIntent(ActivityIntent.StartGpsTracking("running"))
        // 10 fixes, 111 m and 30 s apart (a 4:30 min/km pace)
        for (i in 0..10) {
            now = i * 30_000L
            vm.processIntent(ActivityIntent.Tick(now))
            vm.processIntent(ActivityIntent.AddTrackpoint(GpsTrackpoint(47.0 + i * 0.001, 8.0, 400.0, now)))
        }
        val s = vm.uiState()
        assertEquals(1_112.0, s.liveDistanceMeters, 15.0)
        assertEquals(1, s.liveSplitsSeconds.size) // one full kilometer
        assertEquals(270.0, assertNotNull(s.liveCurrentPaceSecondsPerKm), 8.0)
    }

    @Test
    fun theWayWalkedWhilePausedIsNotCounted() = runTest {
        var now = 0L
        val vm = viewModel(FakeActivityRepository(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)), clock = { now })
        vm.processIntent(ActivityIntent.StartGpsTracking("hiking"))
        vm.processIntent(ActivityIntent.AddTrackpoint(fix(0)))
        vm.processIntent(ActivityIntent.AddTrackpoint(fix(1)))
        val before = vm.uiState().liveDistanceMeters

        vm.processIntent(ActivityIntent.PauseGpsTracking)
        vm.processIntent(ActivityIntent.AddTrackpoint(fix(5))) // ignored while paused
        vm.processIntent(ActivityIntent.ResumeGpsTracking)
        vm.processIntent(ActivityIntent.AddTrackpoint(fix(9))) // first point after the break: a jump of ~800 m
        assertEquals(before, vm.uiState().liveDistanceMeters, 1e-6)

        vm.processIntent(ActivityIntent.AddTrackpoint(fix(10)))
        assertEquals(before + 111.2, vm.uiState().liveDistanceMeters, 3.0)
    }

    @Test
    fun elevationGainIsMeasuredFromTheFixes() = runTest {
        val vm = viewModel(FakeActivityRepository(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.StartGpsTracking("hiking"))
        listOf(400.0, 401.0, 408.0, 408.5, 420.0).forEachIndexed { i, alt -> vm.processIntent(ActivityIntent.AddTrackpoint(fix(i, alt))) }
        assertEquals(20.0, vm.uiState().liveElevationGainMeters, 1e-6) // 400 -> 408 -> 420
    }

    @Test
    fun gpsStatusFollowsTheAccuracyAlsoBeforeTheRecordingStarts() = runTest {
        val vm = viewModel(FakeActivityRepository(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        assertEquals(GpsStatus.SEARCHING, vm.uiState().gpsStatus)
        vm.processIntent(ActivityIntent.GpsFixChanged(65f))
        assertEquals(GpsStatus.WEAK, vm.uiState().gpsStatus)
        vm.processIntent(ActivityIntent.GpsFixChanged(6f))
        assertEquals(GpsStatus.GOOD, vm.uiState().gpsStatus)
        vm.processIntent(ActivityIntent.GpsFixChanged(null))
        assertEquals(GpsStatus.SEARCHING, vm.uiState().gpsStatus)
    }

    @Test
    fun discardingARecordingResetsEverythingAndSavesNothing() = runTest {
        val repo = FakeActivityRepository()
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ActivityIntent.StartGpsTracking("running"))
        vm.processIntent(ActivityIntent.AddTrackpoint(fix(0)))
        vm.processIntent(ActivityIntent.AddTrackpoint(fix(1)))
        vm.processIntent(ActivityIntent.DiscardRecording)

        val s = vm.uiState()
        assertFalse(s.isTracking)
        assertTrue(s.liveTrackpoints.isEmpty())
        assertEquals(0.0, s.liveDistanceMeters)
        assertNull(repo.saved)
    }

    @Test
    fun savedRecordingKeepsRouteAndMovingTime() = runTest {
        var now = 0L
        val repo = FakeActivityRepository()
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)), clock = { now })
        vm.processIntent(ActivityIntent.StartGpsTracking("running"))
        for (i in 0..4) {
            now = i * 10_000L
            vm.processIntent(ActivityIntent.Tick(now))
            vm.processIntent(ActivityIntent.AddTrackpoint(fix(i)))
        }
        vm.processIntent(ActivityIntent.StopAndSaveGpsTracking("u1", null, "Run"))

        val saved = assertNotNull(repo.saved)
        assertEquals(40.0, saved.durationSeconds)
        assertEquals(5, repo.savedTrackpoints?.size) // the repository turns these into the stored polyline
        assertEquals(ActivitySuccess.RECORDED, vm.uiState().success)
        assertTrue(vm.uiState().liveSplitsSeconds.isEmpty())
    }

    @Test
    fun hikingKeepsOnePointEveryThreeSecondsAndCyclingEverySecond() = runTest {
        fun points(seconds: Int) = (0..seconds).map { GpsTrackpoint(47.0 + it * 0.00001, 8.0, 400.0, it * 1000L) }

        val hiking = viewModel(FakeActivityRepository(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        hiking.processIntent(ActivityIntent.StartGpsTracking("hiking"))
        points(9).forEach { hiking.processIntent(ActivityIntent.AddTrackpoint(it)) } // a fix every second
        assertEquals(listOf(0L, 3_000L, 6_000L, 9_000L), hiking.uiState().liveTrackpoints.map { it.timestamp })

        val cycling = viewModel(FakeActivityRepository(), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        cycling.processIntent(ActivityIntent.StartGpsTracking("cycling"))
        points(9).forEach { cycling.processIntent(ActivityIntent.AddTrackpoint(it)) }
        assertEquals(10, cycling.uiState().liveTrackpoints.size)
    }
}
