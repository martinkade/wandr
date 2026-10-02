package com.wandr.presentation.notifications

import com.wandr.domain.model.AppNotification
import com.wandr.domain.model.NotificationType
import com.wandr.domain.model.SocialEntityType
import com.wandr.domain.repository.NotificationRepository
import com.wandr.domain.usecase.GetNotificationsUseCase
import com.wandr.domain.usecase.GetUnreadNotificationCountUseCase
import com.wandr.domain.usecase.MarkNotificationsReadUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeNotificationRepository(val items: List<AppNotification>, var unread: Int = items.count { !it.isRead }) : NotificationRepository {
    var markFails = false
    val marked = mutableListOf<String>()
    override suspend fun getNotifications(userId: String, limit: Int) = Result.success(items)
    override suspend fun getUnreadCount(userId: String) = Result.success(unread)
    override suspend fun markRead(ids: List<String>): Result<Unit> {
        if (markFails) return Result.failure(IllegalStateException("offline"))
        marked += ids
        return Result.success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    private fun n(id: String, read: Boolean) = AppNotification(
        id, NotificationType.LIKE, "x", "Alex", null, SocialEntityType.ACTIVITY, "a1", null, null, null, read, 0L
    )

    private fun kotlinx.coroutines.test.TestScope.vm(repo: FakeNotificationRepository) = NotificationsViewModel(
        GetNotificationsUseCase(repo), GetUnreadNotificationCountUseCase(repo), MarkNotificationsReadUseCase(repo),
        CoroutineScope(UnconfinedTestDispatcher(testScheduler))
    ).also { it.processIntent(NotificationsIntent.Load("me")) }

    @Test
    fun loadsListAndUnreadBadgeBeyondThePage() = runTest {
        val viewModel = vm(FakeNotificationRepository(listOf(n("1", false), n("2", true)), unread = 7))
        assertEquals(2, viewModel.state.value.notifications.size)
        assertEquals(7, viewModel.state.value.unreadCount)
    }

    @Test
    fun markAllReadOnlyTouchesUnreadOnes() = runTest {
        val repo = FakeNotificationRepository(listOf(n("1", false), n("2", true), n("3", false)))
        val viewModel = vm(repo)
        viewModel.processIntent(NotificationsIntent.MarkAllRead)
        assertEquals(listOf("1", "3"), repo.marked)
        assertTrue(viewModel.state.value.notifications.all { it.isRead })
        assertEquals(0, viewModel.state.value.unreadCount)
    }

    @Test
    fun failedMarkingIsReverted() = runTest {
        val repo = FakeNotificationRepository(listOf(n("1", false))).apply { markFails = true }
        val viewModel = vm(repo)
        viewModel.processIntent(NotificationsIntent.MarkRead("1"))
        assertEquals(false, viewModel.state.value.notifications.single().isRead)
        assertEquals(1, viewModel.state.value.unreadCount)
        assertEquals("offline", viewModel.state.value.errorMessage)
    }
}
