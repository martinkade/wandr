package com.wandr.presentation.settings

import com.wandr.domain.model.NotificationPreferences
import com.wandr.domain.model.NotificationScope
import com.wandr.domain.push.PushTokenManager
import com.wandr.domain.repository.AccountRepository
import com.wandr.domain.repository.NotificationPreferencesRepository
import com.wandr.domain.repository.PushTokenRepository
import com.wandr.domain.settings.AppSettings
import com.wandr.domain.settings.PreferenceStore
import com.wandr.domain.usecase.DeleteAccountUseCase
import com.wandr.domain.usecase.GetNotificationPreferencesUseCase
import com.wandr.domain.usecase.RegisterPushTokenUseCase
import com.wandr.domain.usecase.ResetAccountUseCase
import com.wandr.domain.usecase.SaveNotificationPreferencesUseCase
import com.wandr.domain.usecase.UnregisterPushTokenUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class MemoryStore : PreferenceStore {
    val values = mutableMapOf<String, Boolean>()
    override fun getBoolean(key: String, default: Boolean) = values[key] ?: default
    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
}

private class TokenServer : PushTokenRepository {
    var online = true
    val tokens = mutableSetOf<String>()
    override suspend fun register(token: String, platform: String): Result<Unit> {
        if (!online) return Result.failure(IllegalStateException("offline"))
        tokens += token
        return Result.success(Unit)
    }
    override suspend fun unregister(token: String): Result<Unit> {
        if (!online) return Result.failure(IllegalStateException("offline"))
        tokens -= token
        return Result.success(Unit)
    }
}

private class PreferencesServer : NotificationPreferencesRepository {
    var online = true
    var stored = NotificationPreferences()
    override suspend fun get(userId: String) =
        if (online) Result.success(stored) else Result.failure(IllegalStateException("offline"))

    override suspend fun save(userId: String, preferences: NotificationPreferences): Result<Unit> {
        if (!online) return Result.failure(IllegalStateException("offline"))
        stored = preferences
        return Result.success(Unit)
    }
}

private class AccountServer : AccountRepository {
    var online = true
    val calls = mutableListOf<String>()
    override suspend fun resetData(userId: String): Result<Unit> {
        if (!online) return Result.failure(IllegalStateException("offline"))
        calls += "reset:$userId"
        return Result.success(Unit)
    }

    override suspend fun deleteAccount(userId: String): Result<Unit> {
        if (!online) return Result.failure(IllegalStateException("offline"))
        calls += "delete:$userId"
        return Result.success(Unit)
    }
}

class SettingsViewModelTest {
    private class Setup(scope: TestScope) {
        val store = MemoryStore()
        val settings = AppSettings(store)
        val server = TokenServer()
        val dispatcher = UnconfinedTestDispatcher(scope.testScheduler)
        val manager = PushTokenManager(
            RegisterPushTokenUseCase(server), UnregisterPushTokenUseCase(server), CoroutineScope(dispatcher),
            isPushEnabled = { settings.pushNotificationsEnabled }
        )
        val preferences = PreferencesServer()
        val account = AccountServer()
        val vm = SettingsViewModel(
            settings,
            manager,
            GetNotificationPreferencesUseCase(preferences),
            SaveNotificationPreferencesUseCase(preferences),
            ResetAccountUseCase(account),
            DeleteAccountUseCase(account, manager),
            CoroutineScope(dispatcher)
        )
    }

    @Test
    fun pushIsOnByDefault() = runTest {
        assertTrue(Setup(this).vm.state.value.pushEnabled)
    }

    @Test
    fun switchingPushOffRemovesTheDeviceFromTheServerAndOnAddsItAgain() = runTest {
        val s = Setup(this)
        s.manager.onTokenReceived("t1", PushTokenManager.PLATFORM_ANDROID)
        assertEquals(setOf("t1"), s.server.tokens)

        s.vm.processIntent(SettingsIntent.SetPushEnabled(false))
        assertFalse(s.vm.state.value.pushEnabled)
        assertFalse(s.settings.pushNotificationsEnabled)
        assertEquals(emptySet(), s.server.tokens)

        s.vm.processIntent(SettingsIntent.SetPushEnabled(true))
        assertEquals(setOf("t1"), s.server.tokens)
    }

    @Test
    fun whileOffATokenOfANewStartIsNotRegistered() = runTest {
        val s = Setup(this)
        s.settings.pushNotificationsEnabled = false
        s.manager.onTokenReceived("t1", PushTokenManager.PLATFORM_IOS)
        assertEquals(emptySet(), s.server.tokens)
    }

    @Test
    fun offlineTheChoiceIsKeptAndTheRemovalIsRetriedLater() = runTest {
        val s = Setup(this)
        s.manager.onTokenReceived("t1", PushTokenManager.PLATFORM_ANDROID)
        s.server.online = false
        s.vm.processIntent(SettingsIntent.SetPushEnabled(false))
        assertFalse(s.settings.pushNotificationsEnabled) // stored although the server was not reached
        assertNotNull(s.vm.state.value.error)
        assertEquals(setOf("t1"), s.server.tokens)

        s.server.online = true
        s.manager.registerIfPossible() // e.g. the next start
        assertEquals(emptySet(), s.server.tokens)
    }

    @Test
    fun thePreferencesComeFromTheServerAndAChangeIsSaved() = runTest {
        val s = Setup(this)
        s.preferences.stored = NotificationPreferences(activityLikes = false)
        s.vm.processIntent(SettingsIntent.Load("u1"))
        assertEquals(false, s.vm.state.value.notificationPreferences?.activityLikes)

        s.vm.processIntent(
            SettingsIntent.SetNotificationScope(
                NotificationScope.CHALLENGE_COMMENTS,
                false
            )
        )
        assertEquals(
            NotificationPreferences(activityLikes = false, challengeComments = false),
            s.preferences.stored
        )
        assertEquals(s.preferences.stored, s.vm.state.value.notificationPreferences)
    }

    @Test
    fun aChangeThatCannotBeSavedIsTakenBack() = runTest {
        val s = Setup(this)
        s.vm.processIntent(SettingsIntent.Load("u1"))
        s.preferences.online = false
        s.vm.processIntent(
            SettingsIntent.SetNotificationScope(
                NotificationScope.ACTIVITY_COMMENTS,
                false
            )
        )
        assertEquals(true, s.vm.state.value.notificationPreferences?.activityComments)
        assertNotNull(s.vm.state.value.error)
    }

    @Test
    fun resetKeepsTheAccountAndDeletionReportsTheSignOut() = runTest {
        val s = Setup(this)
        s.vm.processIntent(SettingsIntent.Load("u1"))

        s.vm.processIntent(SettingsIntent.ResetAccount)
        assertEquals(SettingsSuccess.ACCOUNT_RESET, s.vm.state.value.success)
        assertFalse(s.vm.state.value.isResettingAccount)

        s.vm.processIntent(SettingsIntent.ClearMessages)
        s.vm.processIntent(SettingsIntent.DeleteAccount)
        assertEquals(SettingsSuccess.ACCOUNT_DELETED, s.vm.state.value.success)
        assertEquals(listOf("reset:u1", "delete:u1"), s.account.calls)
    }

    @Test
    fun aFailedDeletionKeepsTheUserSignedInAndShowsTheError() = runTest {
        val s = Setup(this)
        s.vm.processIntent(SettingsIntent.Load("u1"))
        s.account.online = false
        s.vm.processIntent(SettingsIntent.DeleteAccount)
        assertNull(s.vm.state.value.success)
        assertNotNull(s.vm.state.value.error)
        assertFalse(s.vm.state.value.isDeletingAccount)
    }

    @Test
    fun withoutASignedInUserNothingHappens() = runTest {
        val s = Setup(this)
        s.vm.processIntent(SettingsIntent.DeleteAccount)
        assertEquals(emptyList(), s.account.calls)
    }
}
