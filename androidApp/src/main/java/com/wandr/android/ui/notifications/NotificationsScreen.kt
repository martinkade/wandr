package com.wandr.android.ui.notifications

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wandr.android.R
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.common.userMessage
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.AppNotification
import com.wandr.domain.model.NotificationType
import com.wandr.domain.model.SocialEntityType
import com.wandr.presentation.notifications.NotificationsIntent
import com.wandr.presentation.notifications.NotificationsState
import com.wandr.presentation.notifications.NotificationsViewModel
import org.koin.compose.koinInject

/**
 * Likes, comments and reactions on the user's activities and challenges. Tapping an item marks it as read and opens
 * the activity / challenge via [onOpen]. Pass the same [viewModel] as the bell button so the badge stays in sync.
 */
@Composable
fun NotificationsScreen(
    userId: String,
    onBack: () -> Unit,
    onOpen: (entityType: SocialEntityType, entityId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(userId) { viewModel.processIntent(NotificationsIntent.Load(userId)) }
    NotificationsScreenContent(
        state = state,
        onBack = onBack,
        onIntent = viewModel::processIntent,
        onOpen = { notification ->
            viewModel.processIntent(NotificationsIntent.MarkRead(notification.id))
            onOpen(notification.entityType, notification.entityId)
        },
        modifier = modifier
    )
}

@Composable
private fun NotificationsScreenContent(
    state: NotificationsState,
    onBack: () -> Unit,
    onIntent: (NotificationsIntent) -> Unit,
    onOpen: (AppNotification) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = state.error?.userMessage()
    LaunchedEffect(errorText) {
        errorText?.let {
            snackbarHostState.showSnackbar(it)
            onIntent(NotificationsIntent.ClearMessages)
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.notifications_title),
        modifier = modifier,
        onBack = onBack,
        actions = {
            if (state.unreadCount > 0) {
                TextButton(onClick = { onIntent(NotificationsIntent.MarkAllRead) }) {
                    Text(stringResource(R.string.notifications_mark_all_read))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier
            .fillMaxSize()
            .padding(padding)) {
            when {
                state.isLoading && state.notifications.isEmpty() ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.notifications.isEmpty() -> Text(
                    text = stringResource(R.string.notifications_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.notifications, key = { it.id }) { notification ->
                        NotificationItem(notification, onClick = { onOpen(notification) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private val previewItems = listOf(
    AppNotification("n1", NotificationType.COMMENT, "u2", "Alex Kim", null, SocialEntityType.ACTIVITY, "a1", "c1", "Great pace today!", null, false, 1_768_435_200_000L),
    AppNotification("n2", NotificationType.LIKE, "u3", "Sam Lee", null, SocialEntityType.ACTIVITY, "a1", null, null, null, true, 1_768_431_600_000L)
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Tablet", widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun NotificationsScreenPreview() {
    WandrTheme {
        NotificationsScreenContent(NotificationsState(notifications = previewItems, unreadCount = 1), onBack = {}, onIntent = {}, onOpen = {})
    }
}

@Preview(name = "Empty", showBackground = true)
@Preview(name = "Empty Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun NotificationsScreenEmptyPreview() {
    WandrTheme { NotificationsScreenContent(NotificationsState(), onBack = {}, onIntent = {}, onOpen = {}) }
}
