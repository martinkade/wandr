package com.wandr.presentation.settings

import com.wandr.domain.error.AppError
import com.wandr.domain.error.asAppError
import com.wandr.domain.model.NotificationPreferences
import com.wandr.domain.model.NotificationScope
import com.wandr.domain.push.PushTokenManager
import com.wandr.domain.settings.AppSettings
import com.wandr.domain.usecase.DeleteAccountUseCase
import com.wandr.domain.usecase.GetNotificationPreferencesUseCase
import com.wandr.domain.usecase.ResetAccountUseCase
import com.wandr.domain.usecase.SaveNotificationPreferencesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SettingsSuccess {
    /** The user's data is gone; the account remains. */
    ACCOUNT_RESET,

    /** The account is gone and this device is signed out: the app returns to the sign-in. */
    ACCOUNT_DELETED
}

data class SettingsState(
    /** Push notifications on this device. */
    val pushEnabled: Boolean = true,
    /** The server is being told about a change of [pushEnabled]. */
    val isUpdatingPush: Boolean = false,
    /** What the user wants to be notified about (inbox and push, all devices); null until loaded. */
    val notificationPreferences: NotificationPreferences? = null,
    val isResettingAccount: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val success: SettingsSuccess? = null,
    val error: AppError? = null
) {
    val isBusyWithAccount: Boolean get() = isResettingAccount || isDeletingAccount
}

sealed interface SettingsIntent {
    data class Load(val userId: String) : SettingsIntent
    data class SetPushEnabled(val enabled: Boolean) : SettingsIntent
    data class SetNotificationScope(val scope: NotificationScope, val enabled: Boolean) :
        SettingsIntent

    data object ResetAccount : SettingsIntent
    data object DeleteAccount : SettingsIntent
    data object ClearMessages : SettingsIntent
}

/**
 * The settings screen.
 * - Push on this device: stored at once; telling the server (adding or removing this device's push token) may fail
 *   offline, then the error is shown and the next app start retries (see [PushTokenManager]).
 * - What to be notified about: stored on the server (the notification triggers read it), applied at once and taken back
 *   when saving fails.
 * - Account reset / deletion: see [ResetAccountUseCase] / [DeleteAccountUseCase].
 */
class SettingsViewModel(
    private val settings: AppSettings,
    private val pushTokenManager: PushTokenManager,
    private val getNotificationPreferences: GetNotificationPreferencesUseCase,
    private val saveNotificationPreferences: SaveNotificationPreferencesUseCase,
    private val resetAccount: ResetAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _state = MutableStateFlow(SettingsState(pushEnabled = settings.pushNotificationsEnabled))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private var userId: String? = null

    fun processIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.Load -> load(intent.userId)
            is SettingsIntent.SetPushEnabled -> setPushEnabled(intent.enabled)
            is SettingsIntent.SetNotificationScope -> setScope(intent.scope, intent.enabled)
            SettingsIntent.ResetAccount -> runAccountAction(delete = false)
            SettingsIntent.DeleteAccount -> runAccountAction(delete = true)
            SettingsIntent.ClearMessages -> _state.update { it.copy(error = null, success = null) }
        }
    }

    private fun load(userId: String) {
        this.userId = userId
        _state.update { it.copy(pushEnabled = settings.pushNotificationsEnabled) }
        scope.launch {
            getNotificationPreferences(userId)
                .onSuccess { prefs -> _state.update { it.copy(notificationPreferences = prefs) } }
                .onFailure { error -> _state.update { it.copy(error = error.asAppError()) } }
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

    private fun setScope(notificationScope: NotificationScope, enabled: Boolean) {
        val userId = userId ?: return
        val before = _state.value.notificationPreferences ?: return
        val after = before.with(notificationScope, enabled)
        if (after == before) return
        _state.update { it.copy(notificationPreferences = after, error = null) }
        scope.launch {
            saveNotificationPreferences(userId, after).onFailure { error ->
                // Only take it back if nothing else changed meanwhile.
                _state.update {
                    if (it.notificationPreferences == after) it.copy(
                        notificationPreferences = before,
                        error = error.asAppError()
                    )
                    else it.copy(error = error.asAppError())
                }
            }
        }
    }

    private fun runAccountAction(delete: Boolean) {
        val userId = userId ?: return
        if (_state.value.isBusyWithAccount) return
        _state.update {
            it.copy(
                isResettingAccount = !delete,
                isDeletingAccount = delete,
                error = null,
                success = null
            )
        }
        scope.launch {
            val result = if (delete) deleteAccount(userId) else resetAccount(userId)
            result
                .onSuccess {
                    _state.update {
                        it.copy(
                            isResettingAccount = false,
                            isDeletingAccount = false,
                            // A reset also reset the preferences on the server: all on again.
                            notificationPreferences = NotificationPreferences(),
                            success = if (delete) SettingsSuccess.ACCOUNT_DELETED else SettingsSuccess.ACCOUNT_RESET
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isResettingAccount = false,
                            isDeletingAccount = false,
                            error = error.asAppError()
                        )
                    }
                }
        }
    }
}
