package com.wandr.android.ui.activity

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.wandr.android.R
import com.wandr.android.health.HealthConnectImporter
import com.wandr.android.ui.permission.PermissionDeniedDialog
import com.wandr.android.ui.permission.PermissionDisclosureDialog
import com.wandr.domain.watch.WatchWorkoutInbox
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private enum class Step { Idle, Disclosure, Denied }

/**
 * Top bar button that imports workouts from Health Connect (read-only). A disclosure dialog explains why the data is
 * needed before the permission screen of Health Connect opens. The workouts go into the same inbox as watch workouts,
 * so overlaps with existing activities open the conflict wizard.
 */
@Composable
fun HealthConnectImportAction(inbox: WatchWorkoutInbox = koinInject()) {
    val context = LocalContext.current
    val importer = remember { HealthConnectImporter(context) }
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(Step.Idle) }

    val unavailable = stringResource(R.string.health_connect_unavailable)
    val failed = stringResource(R.string.health_connect_failed)
    val foundMessage: (Int) -> String = { count ->
        if (count == 0) context.getString(R.string.health_connect_nothing_new)
        else context.resources.getQuantityString(R.plurals.health_workouts_found, count, count)
    }

    fun import() {
        scope.launch {
            runCatching { importer.readWorkouts() }
                .onSuccess { workouts ->
                    workouts.forEach(inbox::offer)
                    Toast.makeText(context, foundMessage(workouts.size), Toast.LENGTH_LONG).show()
                }
                .onFailure { Toast.makeText(context, failed, Toast.LENGTH_LONG).show() }
        }
    }

    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        if (granted.containsAll(importer.permissions)) import() else step = Step.Denied
    }

    TextButton(onClick = {
        if (!importer.isAvailable) {
            Toast.makeText(context, unavailable, Toast.LENGTH_LONG).show()
        } else {
            scope.launch { if (importer.hasPermissions()) import() else step = Step.Disclosure }
        }
    }) { Text(stringResource(R.string.health_connect_import_button)) }

    when (step) {
        Step.Disclosure -> PermissionDisclosureDialog(
            title = stringResource(R.string.health_connect_disclosure_title),
            message = stringResource(R.string.health_connect_disclosure_message),
            onContinue = {
                step = Step.Idle
                launcher.launch(importer.permissions)
            },
            onDismiss = { step = Step.Idle }
        )
        Step.Denied -> PermissionDeniedDialog(
            message = stringResource(R.string.health_connect_denied),
            onOpenSettings = {
                step = Step.Idle
                // Health Connect's settings, where the permission can be allowed again.
                runCatching { context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            },
            onDismiss = { step = Step.Idle }
        )
        Step.Idle -> Unit
    }
}
