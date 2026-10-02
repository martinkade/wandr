package com.wandr.domain.push

import com.wandr.domain.repository.PushTokenRepository
import com.wandr.domain.usecase.RegisterPushTokenUseCase
import com.wandr.domain.usecase.UnregisterPushTokenUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakePushTokenRepository : PushTokenRepository {
    var signedIn = false
    val registered = mutableListOf<Pair<String, String>>()
    val unregistered = mutableListOf<String>()
    override suspend fun register(token: String, platform: String): Result<Unit> {
        if (!signedIn) return Result.failure(IllegalStateException("not signed in"))
        registered += token to platform
        return Result.success(Unit)
    }
    override suspend fun unregister(token: String): Result<Unit> {
        unregistered += token
        return Result.success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PushTokenManagerTest {
    private fun kotlinx.coroutines.test.TestScope.manager(repo: FakePushTokenRepository) = PushTokenManager(
        RegisterPushTokenUseCase(repo), UnregisterPushTokenUseCase(repo), CoroutineScope(UnconfinedTestDispatcher(testScheduler))
    )

    @Test
    fun tokenReceivedBeforeSignInIsRegisteredAfterwards() = runTest {
        val repo = FakePushTokenRepository()
        val manager = manager(repo)
        manager.onTokenReceived("t1", PushTokenManager.PLATFORM_ANDROID)
        assertEquals(0, repo.registered.size)

        repo.signedIn = true
        manager.registerIfPossible()
        manager.registerIfPossible() // already registered: no second call
        assertEquals(listOf("t1" to "android"), repo.registered)
    }

    @Test
    fun refreshedTokenIsRegisteredAgainAndSignOutUnregistersIt() = runTest {
        val repo = FakePushTokenRepository().apply { signedIn = true }
        val manager = manager(repo)
        manager.onTokenReceived("t1", PushTokenManager.PLATFORM_IOS)
        manager.onTokenReceived("t2", PushTokenManager.PLATFORM_IOS)
        assertEquals(listOf("t1", "t2"), repo.registered.map { it.first })

        manager.unregisterCurrent()
        manager.unregisterCurrent() // nothing registered any more
        assertEquals(listOf("t2"), repo.unregistered)
    }
}
