package com.wandr.android.ui.main

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wandr.android.health.HealthAutoSync
import com.wandr.android.push.PushNavigation
import com.wandr.android.push.PushPermissionPrompt
import com.wandr.android.ui.activity.ActivityDetailsScreen
import com.wandr.android.ui.activity.LiveGpsTrackingScreen
import com.wandr.android.ui.activity.WatchImportHost
import com.wandr.android.ui.challenge.ChallengeDetailsScreen
import com.wandr.android.ui.challenge.ChallengeListScreen
import com.wandr.android.ui.common.HeroLayer
import com.wandr.android.ui.common.HeroTransitionState
import com.wandr.android.ui.common.LocalHeroTransition
import com.wandr.android.ui.common.SlideInOverlay
import com.wandr.android.ui.feed.FeedScreen
import com.wandr.android.ui.notifications.NotificationBellAction
import com.wandr.android.ui.notifications.NotificationsScreen
import com.wandr.android.ui.profile.ProfileScreen
import com.wandr.android.ui.team.GroupsScreen
import com.wandr.android.ui.team.TeamDetailsScreen
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.model.SocialEntityType
import com.wandr.presentation.main.MainViewModel
import com.wandr.presentation.notifications.NotificationsIntent
import com.wandr.presentation.notifications.NotificationsViewModel
import org.koin.compose.koinInject

/**
 * Post-login root: resolves the user and role, shows the tab bar and the selected tab. Every tab screen
 * injects its own view model.
 */
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedRequested by rememberSaveable { mutableStateOf(MainTab.Feed) }
    LaunchedEffect(Unit) { viewModel.load() }

    // The Groups tab only exists for managers; fall back if the role changes while it is selected.
    val tabs = MainTab.entries.filter { it != MainTab.Groups || state.isManager }
    val selectedTab = if (selectedRequested in tabs) selectedRequested else MainTab.Feed
    val userId = state.userId
    // Group details slide in over the whole main screen, including the tab bar (like a pushed page).
    var openTeamId by rememberSaveable { mutableStateOf<String?>(null) }
    var openChallengeId by rememberSaveable { mutableStateOf<String?>(null) }
    var openActivityId by rememberSaveable { mutableStateOf<String?>(null) }
    var isRecording by rememberSaveable { mutableStateOf(false) }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    // One view model for the bell badge and the list, so reading a notification updates the badge.
    val notificationsViewModel: NotificationsViewModel = koinInject()
    val notifications by notificationsViewModel.state.collectAsState()
    LaunchedEffect(userId) { userId?.let { notificationsViewModel.processIntent(NotificationsIntent.RefreshBadge(it)) } }

    // The badge of a tapped challenge flies from the list item to the details and back (see HeroTransition).
    val heroState = remember { HeroTransitionState() }

    fun openEntity(type: SocialEntityType, id: String) {
        when (type) {
            SocialEntityType.ACTIVITY -> openActivityId = id
            SocialEntityType.CHALLENGE -> {
                heroState.clear() // opened without a tap on a list item: nothing to fly from
                openChallengeId = id
            }
        }
    }

    // A tapped push notification opens what it is about, once the user is known.
    val pushTarget by PushNavigation.target.collectAsState()
    LaunchedEffect(pushTarget, userId) {
        val target = pushTarget ?: return@LaunchedEffect
        if (userId == null) return@LaunchedEffect
        openEntity(SocialEntityType.fromWire(target.entityType), target.entityId)
        PushNavigation.consume()
    }

    CompositionLocalProvider(LocalHeroTransition provides heroState) {
        Box(modifier = modifier) {
        MainScreenContent(
            tabs = tabs,
            selectedTab = selectedTab,
            onSelectTab = { selectedRequested = it },
            isLoading = state.isLoading
        ) { tab ->
            if (userId != null) {
                when (tab) {
                    MainTab.Feed -> FeedScreen(
                        userId = userId,
                        teamId = state.teamId,
                        onOpenActivity = { openActivityId = it },
                        onRecord = { isRecording = true },
                        actions = {
                            NotificationBellAction(
                                unreadCount = notifications.unreadCount,
                                onClick = { showNotifications = true }
                            )
                        }
                    )
                    MainTab.Challenges -> ChallengeListScreen(
                        userId = userId,
                        isManager = state.isManager,
                        onOpenChallenge = { openChallengeId = it }
                    )
                    MainTab.Groups -> GroupsScreen(userId = userId, onOpenTeam = { openTeamId = it })
                    MainTab.Profile -> ProfileScreen(userId = userId, onLogout = { viewModel.logout(onLogout) })
                }
            }
        }

        if (userId != null) {
            WatchImportHost(userId = userId, teamId = state.teamId)
            PushPermissionPrompt()
            HealthAutoSync()
        }

        SlideInOverlay(item = openTeamId.takeIf { userId != null }, onBack = { openTeamId = null }) { teamId ->
            TeamDetailsScreen(teamId = teamId, userId = userId.orEmpty(), onBack = { openTeamId = null })
        }
            SlideInOverlay(
                item = openChallengeId.takeIf { userId != null },
                onBack = { openChallengeId = null },
                onProgress = heroState::onProgress
            ) { challengeId ->
            ChallengeDetailsScreen(
                challengeId = challengeId,
                userId = userId.orEmpty(),
                onBack = { openChallengeId = null }
            )
        }
        SlideInOverlay(item = userId.takeIf { showNotifications }, onBack = { showNotifications = false }) { id ->
            NotificationsScreen(
                userId = id,
                onBack = { showNotifications = false },
                onOpen = { type, entityId -> openEntity(type, entityId) },
                viewModel = notificationsViewModel
            )
        }
        SlideInOverlay(item = openActivityId.takeIf { userId != null }, onBack = { openActivityId = null }) { activityId ->
            ActivityDetailsScreen(activityId = activityId, userId = userId.orEmpty(), onBack = { openActivityId = null })
        }
        // Back is handled by the screen itself: it is blocked while a recording is running.
        SlideInOverlay(item = userId.takeIf { isRecording }, onBack = { }) { id ->
            LiveGpsTrackingScreen(userId = id, teamId = state.teamId, onClose = { isRecording = false })
        }
            // The flying badge, above all pages.
            HeroLayer(heroState)
        }
    }
}

/**
 * Stateless shell of the main screen: the bottom tab bar plus a slot for the selected tab's content.
 * Each tab screen owns its top app bar, so none is drawn here.
 */
@Composable
private fun MainScreenContent(
    tabs: List<MainTab>,
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    tabContent: @Composable (MainTab) -> Unit
) {
    Scaffold(
        modifier = modifier,
        // Each tab screen handles the status bar itself (ScreenScaffold).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { onSelectTab(tab) },
                        icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier
            .fillMaxSize()
            .padding(bottom = padding.calculateBottomPadding())) {
            if (isLoading) CircularProgressIndicator(Modifier.align(Alignment.Center)) else tabContent(selectedTab)
        }
    }
}

@Preview(name = "Manager", showBackground = true)
@Preview(name = "Manager Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Manager Tablet", widthDp = 840, heightDp = 600, showBackground = true)
@Composable
private fun MainContentManagerPreview() {
    WandrTheme {
        MainScreenContent(
            tabs = MainTab.entries, selectedTab = MainTab.Groups, onSelectTab = {}, isLoading = false
        ) { tab -> Text(tab.name) }
    }
}

@Preview(name = "Member", showBackground = true)
@Preview(name = "Loading Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun MainContentMemberPreview() {
    WandrTheme {
        MainScreenContent(
            tabs = MainTab.entries - MainTab.Groups, selectedTab = MainTab.Feed, onSelectTab = {}, isLoading = false
        ) { tab -> Text(tab.name) }
    }
}
