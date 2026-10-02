package com.wandr.presentation.activity

import com.wandr.domain.model.Activity
import com.wandr.domain.model.ConflictResolution
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.repository.ActivityRepository
import com.wandr.domain.usecase.CreateManualActivityUseCase
import com.wandr.domain.usecase.DeleteActivityUseCase
import com.wandr.domain.usecase.GetActivityTrackUseCase
import com.wandr.domain.usecase.GetActivityUseCase
import com.wandr.domain.usecase.GetTeamActivitiesUseCase
import com.wandr.domain.usecase.GetUserActivitiesUseCase
import com.wandr.domain.usecase.RecordGpsActivityUseCase
import com.wandr.domain.usecase.RefreshActivitiesUseCase
import com.wandr.domain.repository.ActivityFeedRepository
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

private object NoFeed : ActivityFeedRepository {
    override suspend fun refreshTeam(teamId: String) = Result.success(Unit)
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

    private fun viewModel(repo: FakeActivityRepository, scope: CoroutineScope) = ActivityViewModel(
        GetUserActivitiesUseCase(repo), GetTeamActivitiesUseCase(repo), GetActivityUseCase(repo),
        GetActivityTrackUseCase(repo), CreateManualActivityUseCase(repo), UpdateActivityUseCase(repo),
        RecordGpsActivityUseCase(repo), DeleteActivityUseCase(repo), RefreshActivitiesUseCase(NoFeed), scope
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

        assertEquals("Title cannot be empty", vm.uiState().errorMessage)
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
        assertEquals("Only the owner can change this activity", vm.uiState().errorMessage)
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
        assertNull(vm.uiState().errorMessage)
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
}
