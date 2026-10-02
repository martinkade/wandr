package com.wandr.wear.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import android.content.pm.PackageManager
import com.wandr.wear.permissions.RequiredPermissions
import com.wandr.wear.ui.component.PermissionDeniedDialog
import com.wandr.wear.ui.component.PermissionDisclosureDialog
import com.wandr.wear.ui.screen.StartScreen
import com.wandr.wear.ui.screen.SummaryScreen
import com.wandr.wear.ui.screen.TypePickerScreen
import com.wandr.wear.ui.screen.WorkoutScreen
import com.wandr.wear.ui.theme.WandrWearTheme

private enum class PermissionStep { NONE, DISCLOSURE, DENIED }

/** Root: wires the state holder to the stateless screens and runs the permission flow (disclosure, prompt, settings). */
@Composable
fun WearApp(viewModel: WearViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var step by remember { mutableStateOf(PermissionStep.NONE) }
    val required = remember { RequiredPermissions.forSdk(Build.VERSION.SDK_INT) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        // Notifications are optional; everything else is required to record.
        val missing = required.filter { it != RequiredPermissions.POST_NOTIFICATIONS && result[it] != true }
        if (missing.isEmpty()) {
            step = PermissionStep.NONE
            viewModel.onIntent(WearIntent.StartWorkout)
        } else {
            step = PermissionStep.DENIED
        }
    }

    fun onStartRequested() {
        val allGranted = required.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (allGranted) viewModel.onIntent(WearIntent.StartWorkout) else step = PermissionStep.DISCLOSURE
    }

    BackHandler(enabled = state.screen is WearScreen.Start) { viewModel.onIntent(WearIntent.Back) }

    WandrWearTheme {
        AppScaffold {
            when (val screen = state.screen) {
                WearScreen.TypePicker -> TypePickerScreen(onSelect = { viewModel.onIntent(WearIntent.SelectType(it)) })
                is WearScreen.Start -> StartScreen(
                    type = screen.type,
                    busy = state.busy,
                    startFailed = state.startFailed,
                    onStart = ::onStartRequested
                )
                WearScreen.Workout -> WorkoutScreen(
                    live = state.live,
                    busy = state.busy,
                    onPause = { viewModel.onIntent(WearIntent.Pause) },
                    onResume = { viewModel.onIntent(WearIntent.Resume) },
                    onStop = { viewModel.onIntent(WearIntent.Stop) }
                )
                is WearScreen.Summary -> SummaryScreen(
                    workout = screen.workout,
                    sendState = state.summarySendState,
                    onDone = { viewModel.onIntent(WearIntent.Done) }
                )
            }
            PermissionDisclosureDialog(
                visible = step == PermissionStep.DISCLOSURE,
                onContinue = { launcher.launch(required.toTypedArray()) },
                onCancel = { step = PermissionStep.NONE }
            )
            PermissionDeniedDialog(
                visible = step == PermissionStep.DENIED,
                onOpenSettings = {
                    step = PermissionStep.NONE
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                onDismiss = { step = PermissionStep.NONE }
            )
        }
    }
}
