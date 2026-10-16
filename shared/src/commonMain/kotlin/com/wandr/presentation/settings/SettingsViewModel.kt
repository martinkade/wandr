package com.wandr.presentation.settings

import com.wandr.domain.error.AppError
import com.wandr.domain.error.asAppError
import com.wandr.domain.push.PushTokenManager
import com.wandr.domain.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    /** Push notifications on this device. */
    val pushEnabled: Boolean = true,
    /** The server is being told about a change of [pushEnabled]. */
    val isUpdatingPush: Boolean = false,
    val error: AppError? = null
)

sealed interface SettingsIntent {
    data object Load : SettingsIntent
    data class SetPushEnabled(val enabled: Boolean) : SettingsIntent
    data object ClearMessages : SettingsIntent
}

/**
 * The settings screen. The choice is stored on the device at once; telling the server (adding or removing this device's
 * push token) may fail offline, then the error is shown and the next app start retries (see [PushTokenManager]).
 */
class SettingsViewModel(
    private val settings: AppSettings,
    private val pushTokenManager: PushTokenManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _state = MutableStateFlow(SettingsState(pushEnabled = settings.pushNotificationsEnabled))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Load -> _state.update { it.copy(pushEnabled = settings.pushNotificationsEnabled) }
            is SettingsIntent.SetPushEnabled -> setPushEnabled(intent.enabled)
            SettingsIntent.ClearMessages -> _state.update { it.copy(error = null) }
        }
    }

    private fun setPushEnabled(enabled: Boolean) {
        if (enabled == settings.pushNotificationsEnabled) return
        settings.pushNotificationsEnabled = enabled
        _state.update { it.copy(pushEnabled = enabled, isUpdatingPush = true, error = null) }
        scope.launch {
            pushTokenManager.applyPushSetting()
                .onSuccess { _state.update { it.copy(isUpdatingPush = false) } }
                .onFailure { error -> _state.update { it.copy(isUpdatingPush = false, error = error.asAppError()) } }
        }
    }
}
