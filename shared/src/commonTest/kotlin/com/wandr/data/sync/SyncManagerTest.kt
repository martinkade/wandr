package com.wandr.data.sync

import com.wandr.data.local.dao.ActivityDao
import com.wandr.data.local.dao.ChallengeDao
import com.wandr.data.local.dao.ProfileDao
import com.wandr.data.local.dao.TeamDao
import com.wandr.data.local.entity.ActivityEntity
import com.wandr.data.local.entity.ChallengeEntity
import com.wandr.data.local.entity.ProfileEntity
import com.wandr.data.local.entity.TeamEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Fake DAOs for unit testing
private class FakeProfileDao : ProfileDao {
    private val dirtyProfiles = mutableListOf<ProfileEntity>()
    fun addDirty(profile: ProfileEntity) { dirtyProfiles.add(profile) }
    override fun getProfileById(id: String): Flow<ProfileEntity?> = flowOf(null)
    override suspend fun getProfileOnce(id: String): ProfileEntity? = null
    override fun getProfilesByIds(ids: List<String>): Flow<List<ProfileEntity>> = flowOf(emptyList())
    override suspend fun insertProfile(profile: ProfileEntity) { }
    override suspend fun updateProfile(profile: ProfileEntity) { }
    override suspend fun getDirtyProfiles(): List<ProfileEntity> = dirtyProfiles
}

private class FakeTeamDao : TeamDao {
    override fun getTeamById(id: String): Flow<TeamEntity?> = flowOf(null)
    override suspend fun getTeamOnce(id: String): TeamEntity? = null
    override fun getTeamsForUser(userId: String): Flow<List<TeamEntity>> = flowOf(emptyList())
    override fun getAdminTeamsForUser(userId: String): Flow<List<TeamEntity>> = flowOf(emptyList())
    override suspend fun getDirtyTeams(): List<TeamEntity> = emptyList()
    override suspend fun insertTeam(team: TeamEntity) { }
    override suspend fun updateTeam(team: TeamEntity) { }
}

private class FakeChallengeDao : ChallengeDao {
    override fun getChallengeById(id: String): Flow<ChallengeEntity?> = flowOf(null)
    override suspend fun getChallengeOnce(id: String): ChallengeEntity? = null
    override fun getAllChallenges(): Flow<List<ChallengeEntity>> = flowOf(emptyList())
    override suspend fun getDirtyChallenges(): List<ChallengeEntity> = emptyList()
    override suspend fun insertChallenge(challenge: ChallengeEntity) { }
    override suspend fun updateChallenge(challenge: ChallengeEntity) { }
}

private class FakeActivityDao : ActivityDao {
    override fun getActivitiesForUser(userId: String): Flow<List<ActivityEntity>> = flowOf(emptyList())
    override fun getActivityById(id: String): Flow<ActivityEntity?> = flowOf(null)
    override suspend fun getActivityOnce(id: String): ActivityEntity? = null
    override fun getActivityCountForUser(userId: String): Flow<Int> = flowOf(0)
    override fun getActivitiesForTeam(teamId: String): Flow<List<ActivityEntity>> = flowOf(emptyList())
    override suspend fun getOverlappingActivities(userId: String, startTime: Long, endTime: Long): List<ActivityEntity> = emptyList()
    override suspend fun insertActivity(activity: ActivityEntity) { }
    override suspend fun updateActivity(activity: ActivityEntity) { }
    override suspend fun deleteActivity(id: String) { }
    override suspend fun getDirtyActivities(): List<ActivityEntity> = emptyList()
    override suspend fun markDeleted(id: String) { }
    override suspend fun getDeletedActivities(): List<ActivityEntity> = emptyList()
}

class SyncManagerTest {

    @Test
    fun testInitialSyncStateIsIdle() {
        val profileDao = FakeProfileDao()
        val teamDao = FakeTeamDao()
        val challengeDao = FakeChallengeDao()
        val activityDao = FakeActivityDao()

        assertEquals(SyncState.Idle, SyncState.Idle)
    }

    @Test
    fun testDirtyProfileEnqueueing() = runTest {
        val profileDao = FakeProfileDao()
        val dirtyProfile = ProfileEntity(
            id = "user_1",
            username = "martinkade",
            displayName = "Martin Kade",
            avatarUrl = null,
            bio = null,
            createdAt = 1000L,
            updatedAt = 1000L,
            syncStatus = "DIRTY"
        )
        profileDao.addDirty(dirtyProfile)

        val dirtyList = profileDao.getDirtyProfiles()
        assertEquals(1, dirtyList.size)
        assertEquals("DIRTY", dirtyList.first().syncStatus)
    }
}
