package com.wandr.android.ui.profile.components

import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.wandr.android.R
import com.wandr.android.health.DailyHealthSummary
import com.wandr.android.health.HealthConnectImporter
import com.wandr.android.ui.permission.PermissionDeniedDialog
import com.wandr.android.ui.permission.PermissionDisclosureDialog
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.domain.watch.WatchWorkoutInbox
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private enum class Step { Idle, Disclosure, Denied }

/**
 * Card that connects Health Connect (read-only) and opens its settings once connected. A disclosure dialog explains why
 * the data is needed before the permission screen of Health Connect opens. Workouts are synced automatically when the
 * app is resumed (see `HealthAutoSync`); they go into the same inbox as watch workouts, so overlaps with existing
 * activities open the conflict wizard.
 */
@Composable
fun HealthImportCard(modifier: Modifier = Modifier, inbox: WatchWorkoutInbox = koinInject()) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val importer = remember { HealthConnectImporter(context) }
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(Step.Idle) }
    var summary by remember { mutableStateOf<DailyHealthSummary?>(null) }

    suspend fun refreshSummary() {
        summary =
            if (importer.isAvailable) runCatching { importer.readToday() }.getOrNull() else null
    }
    LaunchedEffect(Unit) { refreshSummary() }

    val unavailable = stringResource(R.string.health_connect_unavailable)
    val failed = stringResource(R.string.health_connect_failed)

    fun import() {
        scope.launch {
            runCatching { importer.readWorkouts() }
                .onSuccess { workouts ->
                    workouts.forEach(inbox::offer)
                    val message =
                        if (workouts.isEmpty()) resources.getString(R.string.health_connect_nothing_new)
                        else resources.getQuantityString(
                            R.plurals.health_workouts_found,
                            workouts.size,
                            workouts.size
                        )
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                .onFailure { Toast.makeText(context, failed, Toast.LENGTH_LONG).show() }
            refreshSummary()
        }
    }

    val launcher =
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            if (granted.containsAll(importer.workoutPermissions)) import() else step = Step.Denied
        }

    fun openHealthConnectSettings() {
        // Health Connect's settings, where the permissions can be changed.
        runCatching {
            context.startActivity(
                Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    HealthImportCardContent(
        onSettings = {
            if (!importer.isAvailable) {
                Toast.makeText(context, unavailable, Toast.LENGTH_LONG).show()
            } else {
                // Not connected yet: set it up. Connected: workouts sync automatically, the settings are the way to change it.
                scope.launch {
                    if (importer.shouldRequestPermissions()) step =
                        Step.Disclosure else openHealthConnectSettings()
                }
            }
        },
        modifier = modifier
    )

    when (step) {
        Step.Disclosure -> PermissionDisclosureDialog(
            title = stringResource(R.string.health_connect_disclosure_title),
            message = stringResource(R.string.health_connect_disclosure_message),
            onContinue = {
                step = Step.Idle
                importer.markRequested()
                launcher.launch(importer.permissions)
            },
            onDismiss = { step = Step.Idle }
        )

        Step.Denied -> PermissionDeniedDialog(
            message = stringResource(R.string.health_connect_denied),
            onOpenSettings = {
                step = Step.Idle
                openHealthConnectSettings()
            },
            onDismiss = { step = Step.Idle }
        )

        Step.Idle -> Unit
    }
}

@Composable
private fun HealthImportCardContent(
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Image(
                    painter = painterResource(R.drawable.health_connect_logo),
                    contentDescription = stringResource(R.string.health_card_title),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Text(
                    text = stringResource(R.string.health_card_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.health_settings_button)
                )
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Font Scale 1.5x", fontScale = 1.5f, showBackground = true)
@Preview(name = "Tablet", widthDp = 840, heightDp = 400, showBackground = true)
@Composable
private fun HealthImportCardPreview() {
    WandrTheme {
        HealthImportCardContent(
            onSettings = {},
            modifier = Modifier
                .height(240.dp)
                .aspectRatio(1.25f)
        )
    }
}

@Preview(name = "No permission yet", showBackground = true)
@Composable
private fun HealthImportCardNoSummaryPreview() {
    WandrTheme {
        HealthImportCardContent(
            onSettings = {},
            modifier = Modifier
                .height(240.dp)
                .aspectRatio(1.25f)
        )
    }
}
