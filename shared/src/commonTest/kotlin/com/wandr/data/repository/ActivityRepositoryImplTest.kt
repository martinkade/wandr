package com.wandr.data.repository

import com.wandr.data.fit.FitFileStorage
import com.wandr.data.local.dao.ActivityDao
import com.wandr.data.local.entity.ActivityEntity
import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class InMemoryActivityDao : ActivityDao {
    val rows = mutableMapOf<String, ActivityEntity>()
    override fun getActivitiesForUser(userId: String): Flow<List<ActivityEntity>> = flowOf(rows.values.toList())
    override fun getActivitiesForTeam(teamId: String): Flow<List<ActivityEntity>> = flowOf(emptyList())
    override suspend fun getOverlappingActivities(userId: String, startTime: Long, endTime: Long) = emptyList<ActivityEntity>()
    override suspend fun getActivityOnce(id: String): ActivityEntity? = rows[id]
    override fun getActivityCountForUser(userId: String): Flow<Int> = flowOf(rows.size)
    override suspend fun insertActivity(activity: ActivityEntity) { rows[activity.id] = activity }
    override suspend fun updateActivity(activity: ActivityEntity) { rows[activity.id] = activity }
    override suspend fun deleteActivity(id: String) { rows.remove(id) }
    override suspend fun getDirtyActivities(): List<ActivityEntity> = rows.values.toList()
}

class ActivityRepositoryImplTest {

    private val fileSystem = FakeFileSystem()
    private val storage = FitFileStorage("/app/files/fit".toPath(), fileSystem)
    private val dao = InMemoryActivityDao()
    private val repository = ActivityRepositoryImpl(dao, storage)

    private fun activity(id: String, manual: Boolean) = Activity(
        id = id, userId = "u1", teamId = null, title = "Walk", description = null, activityType = "hiking",
        distanceMeters = 1000.0, durationSeconds = 600.0, elevationGainMeters = 10.0, fitFilePath = null,
        startTime = 0L, endTime = 600_000L, isManualEntry = manual, createdAt = 0L, updatedAt = 0L
    )

    private val trackpoints = listOf(
        GpsTrackpoint(47.0, 11.0, 1000.0, 0L, 1.5f),
        GpsTrackpoint(47.001, 11.001, 1005.0, 5_000L, 1.6f)
    )

    @Test
    fun recordedActivityKeepsItsFitFileOnTheDeviceAndMarksItDirty() = runTest {
        val saved = repository.saveActivity(activity("a1", manual = false), trackpoints).getOrThrow()

        val path = assertNotNull(saved.fitFilePath)
        assertEquals("/app/files/fit/a1.fit", path)
        assertTrue(assertNotNull(storage.read(path)).isNotEmpty())
        assertEquals("DIRTY", dao.rows.getValue("a1").syncStatus)
    }

    @Test
    fun manualEntryHasNoFitFile() = runTest {
        val saved = repository.saveActivity(activity("a2", manual = true), trackpoints = null).getOrThrow()
        assertNull(saved.fitFilePath)
    }

    @Test
    fun deletingAnActivityAlsoDeletesItsLocalFitFile() = runTest {
        val path = assertNotNull(repository.saveActivity(activity("a3", manual = false), trackpoints).getOrThrow().fitFilePath)

        repository.deleteActivity("a3").getOrThrow()

        assertNull(storage.read(path))
        assertNull(dao.rows["a3"])
    }

    @Test
    fun deletingAnActivityWithoutFitFileJustRemovesTheRow() = runTest {
        repository.saveActivity(activity("a4", manual = true), trackpoints = null)
        repository.deleteActivity("a4").getOrThrow()
        assertNull(dao.rows["a4"])
    }
}
