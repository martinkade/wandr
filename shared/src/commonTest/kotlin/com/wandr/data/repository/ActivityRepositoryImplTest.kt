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
    override fun getActivityById(id: String): Flow<ActivityEntity?> = flowOf(rows[id])
    override suspend fun getActivityOnce(id: String): ActivityEntity? = rows[id]
    override fun getActivityCountForUser(userId: String): Flow<Int> = flowOf(rows.size)
    override suspend fun insertActivity(activity: ActivityEntity) { rows[activity.id] = activity }
    override suspend fun updateActivity(activity: ActivityEntity) { rows[activity.id] = activity }
    override suspend fun insertActivities(activities: List<ActivityEntity>) { activities.forEach { rows[it.id] = it } }
    override suspend fun deleteActivity(id: String) { rows.remove(id) }
    override suspend fun getDirtyActivities(): List<ActivityEntity> = rows.values.toList()
    override suspend fun markDeleted(id: String) { rows[id]?.let { rows[id] = it.copy(syncStatus = "DELETED") } }
    override suspend fun getDeletedActivities(): List<ActivityEntity> = rows.values.filter { it.syncStatus == "DELETED" }
    override suspend fun getSyncedActivitiesForUserOnce(userId: String) = rows.values.filter { it.userId == userId && it.syncStatus == "SYNCED" }
    override suspend fun getSyncedActivitiesForTeamOnce(teamId: String) = rows.values.filter { it.teamId == teamId && it.syncStatus == "SYNCED" }
}

class ActivityRepositoryImplTest {

    private val fileSystem = FakeFileSystem()
    private val storage = FitFileStorage("/app/files/fit".toPath(), fileSystem)
    private val dao = InMemoryActivityDao()
    private var changes = 0
    private val repository = ActivityRepositoryImpl(dao, storage, onLocalChange = { changes++ })

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
        assertEquals("DELETED", dao.rows.getValue("a3").syncStatus) // marked, so the sync can delete it on the server
    }

    @Test
    fun deletingAnActivityWithoutFitFileJustRemovesTheRow() = runTest {
        repository.saveActivity(activity("a4", manual = true), trackpoints = null)
        repository.deleteActivity("a4").getOrThrow()
        assertEquals("DELETED", dao.rows.getValue("a4").syncStatus)
    }

    @Test
    fun trackIsReadBackFromTheLocalFitFile() = runTest {
        repository.saveActivity(activity("a5", manual = false), trackpoints)

        val track = repository.getTrackpoints("a5")

        assertEquals(trackpoints.size, track.size)
        assertEquals(47.0, track.first().latitude, 1e-4)
    }

    @Test
    fun noTrackForManualEntriesOrWhenTheFileIsMissing() = runTest {
        repository.saveActivity(activity("a6", manual = true), trackpoints = null)
        assertEquals(emptyList(), repository.getTrackpoints("a6"))

        val path = assertNotNull(repository.saveActivity(activity("a7", manual = false), trackpoints).getOrThrow().fitFilePath)
        storage.delete(path) // e.g. the activity was recorded on another device
        assertEquals(emptyList(), repository.getTrackpoints("a7"))
        assertEquals(emptyList(), repository.getTrackpoints("unknown"))
    }

    @Test
    fun recordedRouteIsStoredAsEncodedPolylineForTheServer() = runTest {
        val saved = repository.saveActivity(activity("a8", manual = false), trackpoints).getOrThrow()

        val polyline = assertNotNull(saved.polyline)
        assertEquals(dao.rows.getValue("a8").polyline, polyline)
        val decoded = com.wandr.domain.geo.PolylineCodec.decode(polyline)
        assertEquals(2, decoded.size)
        assertEquals(47.001, decoded.last().latitude, 1e-5)
        assertTrue(saved.showMap) // shared on the map unless the owner opts out
    }

    @Test
    fun editingKeepsTheRouteAndAppliesThePrivacySetting() = runTest {
        val recorded = repository.saveActivity(activity("a9", manual = false), trackpoints).getOrThrow()

        val edited = repository.saveActivity(recorded.copy(title = "Renamed", showMap = false), trackpoints = null).getOrThrow()

        assertEquals(recorded.polyline, edited.polyline)
        assertEquals(false, dao.rows.getValue("a9").showMap)
    }

    @Test
    fun manualEntryHasNoRoute() = runTest {
        assertNull(repository.saveActivity(activity("a10", manual = true), trackpoints = null).getOrThrow().polyline)
    }

    @Test
    fun everyLocalChangeAsksForASync() = runTest {
        val saved = repository.saveActivity(activity("a11", manual = true), trackpoints = null).getOrThrow()
        assertEquals(1, changes)
        repository.saveActivity(saved.copy(title = "Edited"), trackpoints = null)
        assertEquals(2, changes)
        repository.deleteActivity("a11")
        assertEquals(3, changes)
    }
}
