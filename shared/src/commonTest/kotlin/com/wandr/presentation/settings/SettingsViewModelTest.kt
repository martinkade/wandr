package com.wandr.presentation.settings

import com.wandr.domain.push.PushTokenManager
import com.wandr.domain.repository.PushTokenRepository
import com.wandr.domain.settings.AppSettings
import com.wandr.domain.settings.PreferenceStore
import com.wandr.domain.usecase.RegisterPushTokenUseCase
import com.wandr.domain.usecase.UnregisterPushTokenUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        val vm = SettingsViewModel(settings, manager, CoroutineScope(dispatcher))
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
}
