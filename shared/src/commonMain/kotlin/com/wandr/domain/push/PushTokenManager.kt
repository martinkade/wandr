package com.wandr.domain.push

import com.wandr.domain.usecase.RegisterPushTokenUseCase
import com.wandr.domain.usecase.UnregisterPushTokenUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps the device's push token (FCM on Android, APNs on iOS) registered for the signed-in user. The platform hands
 * over the token whenever it gets or refreshes one; registering happens as soon as a user is signed in and is retried
 * on [registerIfPossible] (after sign-in) when it failed before. The token is held in memory: both platforms deliver it
 * again on every app start.
 */
class PushTokenManager(
    private val registerPushToken: RegisterPushTokenUseCase,
    private val unregisterPushToken: UnregisterPushTokenUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    /** The user's choice in the settings; while off, this device gets no pushes (its token is not on the server). */
    private val isPushEnabled: () -> Boolean = { true }
) {
    private var token: String? = null
    private var platform: String = PLATFORM_ANDROID
    private var registeredToken: String? = null

    /** [platform] is [PLATFORM_ANDROID] or [PLATFORM_IOS]. */
    fun onTokenReceived(token: String, platform: String) {
        this.token = token
        this.platform = platform
        scope.launch { registerIfPossible() }
    }

    suspend fun registerIfPossible() {
        val current = token ?: return
        if (!isPushEnabled()) {
            // Switched off: make sure the server forgets this device (also retries a removal that failed while offline).
            unregisterPushToken(current).onSuccess { registeredToken = null }
            return
        }
        if (registeredToken == current) return
        // Fails while nobody is signed in; the next call after sign-in tries again.
        registerPushToken(current, platform).onSuccess { registeredToken = current }
    }

    /**
     * Applies the user's choice right away: registers this device again, or removes it from the server. The choice itself
     * is stored by the caller first ([isPushEnabled]); a removal that fails offline is retried at the next start.
     */
    suspend fun applyPushSetting(): Result<Unit> {
        val current = token ?: registeredToken ?: return Result.success(Unit)
        return if (isPushEnabled()) {
            registeredToken = null
            registerPushToken(current, platform).onSuccess { registeredToken = current }
        } else {
            unregisterPushToken(current).onSuccess { registeredToken = null }
        }
    }

    /** The server dropped this device's registration (the account was deleted): register again for the next user. */
    fun forgetRegistration() {
        registeredToken = null
    }

    /** Stops pushes for the signed-in user; call before signing out (the server needs the session to allow it). */
    suspend fun unregisterCurrent() {
        val current = registeredToken ?: return
        unregisterPushToken(current).onSuccess { registeredToken = null }
    }

    companion object {
        const val PLATFORM_ANDROID = "android"
        const val PLATFORM_IOS = "ios"
    }
}
