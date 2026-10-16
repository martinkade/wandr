package com.wandr.android.ui.settings

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wandr.android.R
import com.wandr.android.ui.common.ScreenScaffold
import com.wandr.android.ui.common.sectionItemShape
import com.wandr.android.ui.common.userMessage
import com.wandr.android.ui.permission.rememberPermissionGate
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.presentation.settings.SettingsIntent
import com.wandr.presentation.settings.SettingsState
import com.wandr.presentation.settings.SettingsViewModel
import org.koin.compose.koinInject

/** Settings of the app on this device, opened from the profile. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    // Whether the system lets the app show notifications at all (the user may have blocked them there).
    var systemAllows by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        systemAllows = NotificationManagerCompat.from(context).areNotificationsEnabled()
        viewModel.processIntent(SettingsIntent.Load)
        onPauseOrDispose { }
    }
    // Switching pushes on asks for the notification permission first (Android 13+), with the disclosure before it.
    val notificationGate = rememberPermissionGate(
        required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList(),
        disclosureTitle = R.string.permission_push_title,
        disclosureMessage = R.string.permission_push_message,
        deniedMessage = R.string.permission_push_denied
    )
    SettingsScreenContent(
        state = state,
        systemAllowsNotifications = systemAllows,
        appVersion = remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
        },
        onPushChange = { enabled ->
            if (enabled) {
                notificationGate.request {
                    systemAllows = NotificationManagerCompat.from(context).areNotificationsEnabled()
                    viewModel.processIntent(SettingsIntent.SetPushEnabled(true))
                }
            } else {
                viewModel.processIntent(SettingsIntent.SetPushEnabled(false))
            }
        },
        onOpenSystemSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        },
        onClearMessages = { viewModel.processIntent(SettingsIntent.ClearMessages) },
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun SettingsScreenContent(
    state: SettingsState,
    systemAllowsNotifications: Boolean,
    appVersion: String,
    onPushChange: (Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit,
    onClearMessages: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = state.error?.userMessage()
    LaunchedEffect(errorText) {
        if (errorText != null) {
            snackbarHostState.showSnackbar(errorText)
            onClearMessages()
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionTitle(stringResource(R.string.settings_notifications_section))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SettingsRow(isFirst = true, isLast = state.pushEnabled.not() || systemAllowsNotifications) {
                    Icon(painterResource(R.drawable.ic_notifications), contentDescription = null)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_push_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.settings_push_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.isUpdatingPush) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    Switch(checked = state.pushEnabled, onCheckedChange = onPushChange, enabled = !state.isUpdatingPush)
                }
                // On in the app, but blocked in the system settings: nothing would show.
                if (state.pushEnabled && !systemAllowsNotifications) {
                    SettingsRow(isFirst = false, isLast = true) {
                        Text(
                            stringResource(R.string.settings_push_blocked),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onOpenSystemSettings) { Text(stringResource(R.string.settings_open_system)) }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.settings_about_section))
            SettingsRow(isFirst = true, isLast = true) {
                Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(appVersion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

/** A row of the settings as a card of a group of connected cards (like the lists in the profile). */
@Composable
private fun SettingsRow(isFirst: Boolean, isLast: Boolean, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(CardDefaults.cardColors().containerColor, sectionItemShape(isFirst, isLast))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true, heightDp = 900)
@Composable
private fun SettingsScreenPreview() {
    WandrTheme {
        SettingsScreenContent(
            state = SettingsState(pushEnabled = true), systemAllowsNotifications = true, appVersion = "1.0.0",
            onPushChange = {}, onOpenSystemSettings = {}, onClearMessages = {}, onBack = {}
        )
    }
}

@Preview(name = "Blocked in the system", showBackground = true)
@Composable
private fun SettingsScreenBlockedPreview() {
    WandrTheme {
        SettingsScreenContent(
            state = SettingsState(pushEnabled = true), systemAllowsNotifications = false, appVersion = "1.0.0",
            onPushChange = {}, onOpenSystemSettings = {}, onClearMessages = {}, onBack = {}
        )
    }
}
