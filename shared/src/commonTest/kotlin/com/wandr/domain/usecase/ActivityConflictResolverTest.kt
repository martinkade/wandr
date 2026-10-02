package com.wandr.domain.usecase

import com.wandr.domain.model.Activity
import com.wandr.domain.model.ActivityConflictException
import com.wandr.domain.model.ConflictResolution
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class OverlapRepository(initial: List<Activity>) : ActivityRepository {
    val stored = initial.toMutableList()
    val tracks = mutableMapOf<String, List<GpsTrackpoint>>()
    var savedTrack: List<GpsTrackpoint>? = null

    override fun getUserActivities(userId: String): Flow<List<Activity>> = emptyFlow()
    override fun getActivityById(id: String): Flow<Activity?> = emptyFlow()
    override suspend fun getTrackpoints(activityId: String): List<GpsTrackpoint> = tracks[activityId].orEmpty()
    override fun getUserActivityCount(userId: String): Flow<Int> = emptyFlow()
    override fun getTeamActivities(teamId: String): Flow<List<Activity>> = emptyFlow()
    override suspend fun getOverlappingActivities(userId: String, startTime: Long, endTime: Long): List<Activity> =
        stored.filter { it.userId == userId && it.startTime < endTime && it.endTime > startTime }
    override suspend fun saveActivity(activity: Activity, trackpoints: List<GpsTrackpoint>?): Result<Activity> {
        stored.removeAll { it.id == activity.id }
        stored += activity
        savedTrack = trackpoints
        return Result.success(activity)
    }
    override suspend fun deleteActivity(id: String): Result<Unit> {
        stored.removeAll { it.id == id }
        return Result.success(Unit)
    }
}

class ActivityConflictResolverTest {

    private fun activity(id: String, start: Long, end: Long, km: Double = 10.0, manual: Boolean = true) = Activity(
        id = id, userId = "u1", teamId = null, title = id, description = null, activityType = "running",
        distanceMeters = km * 1000, durationSeconds = (end - start) / 1000.0, elevationGainMeters = 100.0,
        fitFilePath = null, startTime = start, endTime = end, isManualEntry = manual, createdAt = 0L, updatedAt = 0L
    )

    @Test
    fun touchingActivitiesDoNotConflict() = runTest {
        val repo = OverlapRepository(listOf(activity("a", 0, 1000)))
        val result = ActivityConflictResolver(repo).save(activity("b", 1000, 2000), null, null)
        assertTrue(result.isSuccess)
        assertEquals(2, repo.stored.size)
    }

    @Test
    fun overlapIsReportedAndNothingIsSaved() = runTest {
        val repo = OverlapRepository(listOf(activity("a", 0, 1000)))
        val error = ActivityConflictResolver(repo).save(activity("b", 500, 1500), null, null).exceptionOrNull()
        val conflict = assertIs<ActivityConflictException>(error)
        assertEquals(listOf("a"), conflict.conflicting.map { it.id })
        assertTrue(conflict.canTrim)
        assertEquals(1, repo.stored.size)
    }

    @Test
    fun theEditedActivityDoesNotConflictWithItself() = runTest {
        val repo = OverlapRepository(listOf(activity("a", 0, 1000)))
        assertTrue(ActivityConflictResolver(repo).save(activity("a", 100, 900), null, null).isSuccess)
    }

    @Test
    fun otherUsersActivitiesAreIgnored() = runTest {
        val repo = OverlapRepository(listOf(activity("a", 0, 1000).copy(userId = "u2")))
        assertTrue(ActivityConflictResolver(repo).save(activity("b", 0, 1000), null, null).isSuccess)
    }

    @Test
    fun mergeCombinesMetricsAndRemovesTheOthers() = runTest {
        val repo = OverlapRepository(listOf(activity("a", 0, 3_600_000, km = 5.0), activity("c", 7_000_000, 9_000_000, km = 1.0)))
        val merged = ActivityConflictResolver(repo)
            .save(activity("b", 1_800_000, 5_400_000, km = 3.0), null, ConflictResolution.MERGE).getOrThrow()

        assertEquals("b", merged.id)
        assertEquals(0L, merged.startTime)
        assertEquals(5_400_000L, merged.endTime)
        assertEquals(8_000.0, merged.distanceMeters)
        assertEquals(200.0, merged.elevationGainMeters)
        assertEquals(5400.0, merged.durationSeconds)
        assertEquals(listOf("c", "b").sorted(), repo.stored.map { it.id }.sorted())
    }

    @Test
    fun mergeJoinsAvailableTracks() = runTest {
        val repo = OverlapRepository(listOf(activity("a", 0, 1000, manual = false)))
        repo.tracks["a"] = listOf(GpsTrackpoint(1.0, 1.0, 0.0, 300L))
        val own = listOf(GpsTrackpoint(2.0, 2.0, 0.0, 100L))
        val merged = ActivityConflictResolver(repo).save(activity("b", 500, 1500, manual = false), own, ConflictResolution.MERGE).getOrThrow()

        assertEquals(listOf(100L, 300L), repo.savedTrack?.map { it.timestamp })
        assertFalse(merged.isManualEntry)
    }

    @Test
    fun trimKeepsTheLongestFreePartAndScalesTheMetrics() = runTest {
        // Candidate 0..10_000 (10 km); existing covers 0..2_000 and 6_000..7_000 -> free: 2_000..6_000 and 7_000..10_000
        val repo = OverlapRepository(listOf(activity("a", 0, 2_000), activity("c", 6_000, 7_000)))
        val trimmed = ActivityConflictResolver(repo)
            .save(activity("b", 0, 10_000, km = 10.0), null, ConflictResolution.TRIM).getOrThrow()

        assertEquals(2_000L, trimmed.startTime)
        assertEquals(6_000L, trimmed.endTime)
        assertEquals(4_000.0, trimmed.distanceMeters, 0.001)
        assertEquals(4.0, trimmed.durationSeconds, 0.001)
        assertEquals(3, repo.stored.size)
    }

    @Test
    fun trimFiltersTheTrackToTheRemainingRange() = runTest {
        val repo = OverlapRepository(listOf(activity("a", 0, 5_000)))
        val track = listOf(1_000L, 4_000L, 6_000L, 9_000L).map { GpsTrackpoint(0.0, 0.0, 0.0, it) }
        ActivityConflictResolver(repo).save(activity("b", 0, 10_000), track, ConflictResolution.TRIM).getOrThrow()
        assertEquals(listOf(6_000L, 9_000L), repo.savedTrack?.map { it.timestamp })
    }

    @Test
    fun fullyCoveredActivityCannotBeTrimmed() = runTest {
        val repo = OverlapRepository(listOf(activity("a", 0, 10_000)))
        val resolver = ActivityConflictResolver(repo)

        val conflict = assertIs<ActivityConflictException>(resolver.save(activity("b", 2_000, 4_000), null, null).exceptionOrNull())
        assertFalse(conflict.canTrim)
        assertTrue(resolver.save(activity("b", 2_000, 4_000), null, ConflictResolution.TRIM).isFailure)
        assertEquals(1, repo.stored.size)
    }

    @Test
    fun noGapMeansNoFreeRange() {
        val resolver = ActivityConflictResolver(OverlapRepository(emptyList()))
        assertNull(resolver.freeRange(activity("b", 0, 100), listOf(activity("a", 0, 100))))
    }
}
