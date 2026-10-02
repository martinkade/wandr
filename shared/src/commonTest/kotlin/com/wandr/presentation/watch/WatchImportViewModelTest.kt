package com.wandr.presentation.watch

import com.wandr.domain.model.Activity
import com.wandr.domain.model.ConflictResolution
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.repository.ActivityRepository
import com.wandr.domain.usecase.ImportWatchWorkoutUseCase
import com.wandr.domain.watch.WatchTrackpoint
import com.wandr.domain.watch.WatchWorkout
import com.wandr.domain.watch.WatchWorkoutInbox
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class WatchFakeRepository(initial: List<Activity> = emptyList()) : ActivityRepository {
    val activities = MutableStateFlow(initial)
    var savedTrack: List<GpsTrackpoint>? = null

    override fun getUserActivities(userId: String): Flow<List<Activity>> = activities
    override fun getActivityById(id: String): Flow<Activity?> = activities.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun getTrackpoints(activityId: String): List<GpsTrackpoint> = emptyList()
    override fun getUserActivityCount(userId: String): Flow<Int> = emptyFlow()
    override fun getTeamActivities(teamId: String): Flow<List<Activity>> = emptyFlow()
    override suspend fun getOverlappingActivities(userId: String, startTime: Long, endTime: Long): List<Activity> =
        activities.value.filter { it.userId == userId && it.startTime < endTime && it.endTime > startTime }
    override suspend fun saveActivity(activity: Activity, trackpoints: List<GpsTrackpoint>?): Result<Activity> {
        savedTrack = trackpoints
        activities.value = activities.value.filterNot { it.id == activity.id } + activity
        return Result.success(activity)
    }
    override suspend fun deleteActivity(id: String): Result<Unit> {
        activities.value = activities.value.filterNot { it.id == id }
        return Result.success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WatchImportViewModelTest {
    private fun workout(id: String, start: Long, end: Long) = WatchWorkout(
        id = id, activityType = "running", startTime = start, endTime = end, distanceMeters = 5_000.0,
        elevationGainMeters = 20.0, trackpoints = listOf(WatchTrackpoint(1.0, 2.0, 3.0, start))
    )

    private val existing = Activity(
        id = "e1", userId = "u1", teamId = null, title = "Existing", description = null, activityType = "hiking",
        distanceMeters = 1_000.0, durationSeconds = 100.0, elevationGainMeters = 0.0, fitFilePath = null,
        startTime = 0L, endTime = 100_000L, isManualEntry = true, createdAt = 0L, updatedAt = 0L
    )

    private data class Setup(val vm: WatchImportViewModel, val repo: WatchFakeRepository, val inbox: WatchWorkoutInbox)

    private fun kotlinx.coroutines.test.TestScope.setup(initial: List<Activity> = emptyList()): Setup {
        val repo = WatchFakeRepository(initial)
        val inbox = WatchWorkoutInbox()
        val vm = WatchImportViewModel(inbox, ImportWatchWorkoutUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(WatchImportIntent.Start("u1", "t1"))
        return Setup(vm, repo, inbox)
    }

    @Test
    fun importsAWorkoutAsRecordedActivity() = runTest {
        val (vm, repo, inbox) = setup()
        val acknowledged = mutableListOf<String>()
        inbox.onHandled = { acknowledged += it }
        inbox.offer(workout("w1", 1_000_000L, 1_600_000L))

        val saved = assertNotNull(repo.activities.value.singleOrNull())
        assertEquals("w1", saved.id)
        assertEquals("u1", saved.userId)
        assertEquals("t1", saved.teamId)
        assertFalse(saved.isManualEntry)
        assertEquals(600.0, saved.durationSeconds)
        assertEquals(1, repo.savedTrack?.size)
        assertEquals(1, vm.state.value.importedCount)
        assertTrue(inbox.pending.value.isEmpty())
        assertEquals(listOf("w1"), acknowledged)
    }

    @Test
    fun aWorkoutThatWasAlreadyImportedIsNotImportedAgain() = runTest {
        val (_, repo, inbox) = setup()
        inbox.offer(workout("w1", 1_000_000L, 1_600_000L))
        inbox.offer(workout("w1", 1_000_000L, 1_600_000L))
        assertEquals(1, repo.activities.value.size)
    }

    @Test
    fun overlapWaitsForTheWizardAndLaterWorkoutsFollow() = runTest {
        val (vm, repo, inbox) = setup(listOf(existing))
        inbox.offer(workout("w1", 50_000L, 150_000L))
        inbox.offer(workout("w2", 500_000L, 600_000L))

        val conflict = assertNotNull(vm.state.value.conflict)
        assertFalse(conflict.dismissible)
        assertEquals("w1", vm.state.value.conflictWorkout?.id)
        // The second workout waits until the first one is resolved; nothing is lost.
        assertEquals(listOf("w1", "w2"), inbox.pending.value.map { it.id })

        vm.processIntent(WatchImportIntent.ResolveConflict(ConflictResolution.MERGE))
        assertNull(vm.state.value.conflict)
        assertEquals(setOf("w1", "w2"), repo.activities.value.map { it.id }.toSet())
        assertTrue(inbox.pending.value.isEmpty())
    }

    @Test
    fun discardDropsTheWorkoutAndKeepsTheExistingActivity() = runTest {
        val (vm, repo, inbox) = setup(listOf(existing))
        inbox.offer(workout("w1", 50_000L, 150_000L))
        vm.processIntent(WatchImportIntent.ResolveConflict(ConflictResolution.DISCARD))

        assertEquals(listOf("e1"), repo.activities.value.map { it.id })
        assertTrue(inbox.pending.value.isEmpty())
        assertNull(vm.state.value.conflict)
    }
}
