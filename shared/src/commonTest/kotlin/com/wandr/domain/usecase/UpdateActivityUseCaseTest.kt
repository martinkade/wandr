package com.wandr.domain.usecase

import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class RecordingActivityRepository : ActivityRepository {
    var saved: Activity? = null
    var trackpoints: List<GpsTrackpoint>? = emptyList()
    override fun getUserActivities(userId: String): Flow<List<Activity>> = emptyFlow()
    override fun getActivityById(id: String): Flow<Activity?> = emptyFlow()
    override suspend fun getTrackpoints(activityId: String): List<GpsTrackpoint> = emptyList()
    override fun getUserActivityCount(userId: String): Flow<Int> = emptyFlow()
    override fun getTeamActivities(teamId: String): Flow<List<Activity>> = emptyFlow()
    override suspend fun getOverlappingActivities(userId: String, startTime: Long, endTime: Long) = emptyList<Activity>()
    override suspend fun saveActivity(activity: Activity, trackpoints: List<GpsTrackpoint>?): Result<Activity> {
        saved = activity
        this.trackpoints = trackpoints
        return Result.success(activity)
    }
    override suspend fun deleteActivity(id: String): Result<Unit> = Result.success(Unit)
}

class UpdateActivityUseCaseTest {
    private val repo = RecordingActivityRepository()
    private val update = UpdateActivityUseCase(repo)

    private val activity = Activity(
        "a1", "u1", null, "Walk", null, "hiking", 1000.0, 600.0, 10.0, "/files/fit/a1.fit", 0L, 600_000L, false, 0L, 0L
    )

    @Test
    fun blankTitleIsRejected() = runTest {
        assertTrue(update(activity.copy(title = "  ")).isFailure)
        assertNull(repo.saved)
    }

    @Test
    fun negativeValuesAreRejected() = runTest {
        assertTrue(update(activity.copy(distanceMeters = -1.0)).isFailure)
        assertTrue(update(activity.copy(durationSeconds = -1.0)).isFailure)
        assertTrue(update(activity.copy(elevationGainMeters = -1.0)).isFailure)
        assertNull(repo.saved)
    }

    @Test
    fun trimsTextKeepsTheTrackLinkAndWritesNoNewTrack() = runTest {
        update(activity.copy(title = "  Walk  ", description = "  \n")).getOrThrow()

        val saved = repo.saved!!
        assertEquals("Walk", saved.title)
        assertNull(saved.description) // blank becomes null
        assertEquals("/files/fit/a1.fit", saved.fitFilePath)
        assertNull(repo.trackpoints) // updating never re-encodes the FIT file
    }
}
