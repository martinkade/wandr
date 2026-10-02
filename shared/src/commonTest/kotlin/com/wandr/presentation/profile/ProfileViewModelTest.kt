package com.wandr.presentation.profile

import com.wandr.domain.model.Activity
import com.wandr.domain.model.GpsTrackpoint
import com.wandr.domain.model.Profile
import com.wandr.domain.repository.ActivityRepository
import com.wandr.domain.usecase.GetUserActivityCountUseCase
import kotlinx.coroutines.flow.emptyFlow
import com.wandr.domain.repository.ProfileRepository
import com.wandr.domain.usecase.GetProfileUseCase
import com.wandr.domain.usecase.RefreshProfileUseCase
import com.wandr.domain.usecase.RemoveAvatarUseCase
import com.wandr.domain.usecase.UpdateProfileUseCase
import com.wandr.domain.usecase.UploadAvatarUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeActivityRepository(count: Int = 0) : ActivityRepository {
    val activityCount = MutableStateFlow(count)

    override fun getUserActivities(userId: String): Flow<List<Activity>> = emptyFlow()
    override fun getActivityById(id: String): Flow<Activity?> = emptyFlow()
    override suspend fun getTrackpoints(activityId: String): List<GpsTrackpoint> = emptyList()
    override fun getUserActivityCount(userId: String): Flow<Int> = activityCount
    override fun getTeamActivities(teamId: String): Flow<List<Activity>> = emptyFlow()
    override suspend fun getOverlappingActivities(userId: String, startTime: Long, endTime: Long): List<Activity> = emptyList()
    override suspend fun saveActivity(activity: Activity, trackpoints: List<GpsTrackpoint>?): Result<Activity> =
        Result.success(activity)
    override suspend fun deleteActivity(id: String): Result<Unit> = Result.success(Unit)
}

private class FakeProfileRepository(initial: Profile?) : ProfileRepository {
    val profile = MutableStateFlow(initial)
    var failAvatar = false
    var refreshed = 0
    var uploaded: ByteArray? = null

    override fun getProfile(userId: String): Flow<Profile?> = profile

    override suspend fun refreshProfile(userId: String): Result<Unit> {
        refreshed++
        return Result.success(Unit)
    }

    override suspend fun updateProfile(profile: Profile): Result<Profile> {
        this.profile.value = profile
        return Result.success(profile)
    }

    override suspend fun setAvatar(userId: String, jpegBytes: ByteArray): Result<Profile> {
        if (failAvatar) return Result.failure(IllegalStateException("offline"))
        uploaded = jpegBytes
        val updated = profile.value!!.copy(avatarUrl = "https://cdn/avatar.jpg")
        profile.value = updated
        return Result.success(updated)
    }

    override suspend fun removeAvatar(userId: String): Result<Profile> {
        val updated = profile.value!!.copy(avatarUrl = null)
        profile.value = updated
        return Result.success(updated)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val profile = Profile(
        id = "u1", username = "martin", displayName = "Martin", avatarUrl = null, bio = null,
        createdAt = 0, updatedAt = 0
    )

    private fun viewModel(
        repo: FakeProfileRepository,
        scope: CoroutineScope,
        activities: FakeActivityRepository = FakeActivityRepository()
    ) = ProfileViewModel(
        GetProfileUseCase(repo), UpdateProfileUseCase(repo), UploadAvatarUseCase(repo),
        RemoveAvatarUseCase(repo), RefreshProfileUseCase(repo), GetUserActivityCountUseCase(activities), scope
    )

    @Test
    fun loadShowsCachedProfileAndTriggersRemoteRefresh() = runTest {
        val repo = FakeProfileRepository(profile)
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ProfileIntent.LoadProfile("u1"))
        assertEquals("Martin", vm.uiState.value.profile?.displayName)
        assertEquals(1, repo.refreshed)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun loadExposesActivityCountAndKeepsItLive() = runTest {
        val activities = FakeActivityRepository(count = 3)
        val vm = viewModel(FakeProfileRepository(profile), CoroutineScope(UnconfinedTestDispatcher(testScheduler)), activities)
        vm.processIntent(ProfileIntent.LoadProfile("u1"))
        assertEquals(3, vm.uiState.value.activityCount)

        activities.activityCount.value = 4
        assertEquals(4, vm.uiState.value.activityCount)
    }

    @Test
    fun uploadAvatarUpdatesProfileAndKeepsUnsavedTextEdits() = runTest {
        val repo = FakeProfileRepository(profile)
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ProfileIntent.LoadProfile("u1"))
        vm.processIntent(ProfileIntent.DisplayNameChanged("Martin K."))

        vm.processIntent(ProfileIntent.UploadAvatar("u1", byteArrayOf(1, 2, 3)))

        val state = vm.uiState.value
        assertEquals("https://cdn/avatar.jpg", state.profile?.avatarUrl)
        assertEquals("Martin K.", state.profile?.displayName)
        assertTrue(state.hasUnsavedChanges)
        assertFalse(state.isAvatarUpdating)
        assertEquals(3, repo.uploaded?.size)
    }

    @Test
    fun failedAvatarUploadSurfacesErrorAndKeepsOldAvatar() = runTest {
        val repo = FakeProfileRepository(profile).apply { failAvatar = true }
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ProfileIntent.LoadProfile("u1"))
        vm.processIntent(ProfileIntent.UploadAvatar("u1", byteArrayOf(1)))

        assertEquals("offline", vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.profile?.avatarUrl)
        assertFalse(vm.uiState.value.isAvatarUpdating)
    }

    @Test
    fun emptyAvatarIsRejectedWithoutCallingRepository() = runTest {
        val repo = FakeProfileRepository(profile)
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ProfileIntent.LoadProfile("u1"))
        vm.processIntent(ProfileIntent.UploadAvatar("u1", ByteArray(0)))

        assertNotNull(vm.uiState.value.errorMessage)
        assertNull(repo.uploaded)
    }

    @Test
    fun removeAvatarClearsUrl() = runTest {
        val repo = FakeProfileRepository(profile.copy(avatarUrl = "https://cdn/a.jpg"))
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ProfileIntent.LoadProfile("u1"))
        vm.processIntent(ProfileIntent.RemoveAvatar("u1"))
        assertNull(vm.uiState.value.profile?.avatarUrl)
    }

    @Test
    fun discardChangesRestoresStoredProfile() = runTest {
        val repo = FakeProfileRepository(profile)
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ProfileIntent.LoadProfile("u1"))
        vm.processIntent(ProfileIntent.DisplayNameChanged("Changed"))
        assertTrue(vm.uiState.value.hasUnsavedChanges)

        vm.processIntent(ProfileIntent.DiscardChanges)

        assertEquals("Martin", vm.uiState.value.profile?.displayName)
        assertFalse(vm.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun saveReportsSuccessAndClearMessagesResetsIt() = runTest {
        val repo = FakeProfileRepository(profile)
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ProfileIntent.LoadProfile("u1"))
        vm.processIntent(ProfileIntent.SaveProfile)
        assertEquals(ProfileSuccess.PROFILE_SAVED, vm.uiState.value.success)

        vm.processIntent(ProfileIntent.ClearMessages)
        assertNull(vm.uiState.value.success)
    }

    @Test
    fun avatarUpdateReportsAvatarSuccess() = runTest {
        val repo = FakeProfileRepository(profile)
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ProfileIntent.LoadProfile("u1"))
        vm.processIntent(ProfileIntent.UploadAvatar("u1", byteArrayOf(1)))
        assertEquals(ProfileSuccess.AVATAR_UPDATED, vm.uiState.value.success)
    }

    @Test
    fun saveClearsUnsavedFlag() = runTest {
        val repo = FakeProfileRepository(profile)
        val vm = viewModel(repo, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        vm.processIntent(ProfileIntent.LoadProfile("u1"))
        vm.processIntent(ProfileIntent.BioChanged("Hiker"))
        vm.processIntent(ProfileIntent.SaveProfile)
        assertFalse(vm.uiState.value.hasUnsavedChanges)
        assertEquals("Hiker", repo.profile.value?.bio)
    }
}
