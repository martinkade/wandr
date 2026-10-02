package com.wandr.presentation.notifications

import com.wandr.domain.model.AppNotification
import com.wandr.domain.usecase.GetNotificationsUseCase
import com.wandr.domain.usecase.GetUnreadNotificationCountUseCase
import com.wandr.domain.usecase.MarkNotificationsReadUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsState(
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface NotificationsIntent {
    data class Load(val userId: String) : NotificationsIntent

    /** Only refreshes the unread badge (cheap). */
    data class RefreshBadge(val userId: String) : NotificationsIntent
    data class MarkRead(val id: String) : NotificationsIntent
    object MarkAllRead : NotificationsIntent
    object ClearMessages : NotificationsIntent
}

class NotificationsViewModel(
    private val getNotifications: GetNotificationsUseCase,
    private val getUnreadCount: GetUnreadNotificationCountUseCase,
    private val markRead: MarkNotificationsReadUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    fun processIntent(intent: NotificationsIntent) {
        when (intent) {
            is NotificationsIntent.Load -> load(intent.userId)
            is NotificationsIntent.RefreshBadge -> scope.launch {
                getUnreadCount(intent.userId).onSuccess { count -> _state.update { it.copy(unreadCount = count) } }
            }
            is NotificationsIntent.MarkRead -> markAsRead(listOf(intent.id))
            is NotificationsIntent.MarkAllRead -> markAsRead(_state.value.notifications.filterNot { it.isRead }.map { it.id })
            is NotificationsIntent.ClearMessages -> _state.update { it.copy(errorMessage = null) }
        }
    }

    private fun load(userId: String) {
        _state.update { it.copy(isLoading = true) }
        scope.launch {
            val list = getNotifications(userId)
            list.onSuccess { notifications ->
                _state.update {
                    it.copy(isLoading = false, notifications = notifications, unreadCount = notifications.count { n -> !n.isRead }, errorMessage = null)
                }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, errorMessage = error.message ?: "Notifications could not be loaded") }
            }
            // The badge counts beyond the loaded page.
            getUnreadCount(userId).onSuccess { count -> _state.update { it.copy(unreadCount = count) } }
        }
    }

    private fun markAsRead(ids: List<String>) {
        val unread = ids.filter { id -> _state.value.notifications.any { it.id == id && !it.isRead } }
        if (unread.isEmpty()) return
        _state.update { s ->
            s.copy(
                notifications = s.notifications.map { if (it.id in unread) it.copy(isRead = true) else it },
                unreadCount = (s.unreadCount - unread.size).coerceAtLeast(0)
            )
        }
        scope.launch {
            markRead(unread).onFailure { error ->
                _state.update { s ->
                    s.copy(
                        notifications = s.notifications.map { if (it.id in unread) it.copy(isRead = false) else it },
                        unreadCount = s.unreadCount + unread.size,
                        errorMessage = error.message ?: "Could not mark as read"
                    )
                }
            }
        }
    }
}
