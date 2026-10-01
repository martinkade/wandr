package com.wandr.android.ui.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * Runs an action only once the required permissions are granted. If they are not, it first shows a
 * disclosure dialog explaining why they are needed, and only then the system permission dialog.
 * When the system will not ask again, a dialog leads to the app settings instead.
 */
@Stable
class PermissionGate internal constructor(
    private val isGranted: () -> Boolean,
    private val onRequest: (onGranted: () -> Unit) -> Unit
) {
    val granted: Boolean get() = isGranted()

    /** Invokes [onGranted] immediately when permitted, otherwise after the user grants the permission. */
    fun request(onGranted: () -> Unit) = onRequest(onGranted)
}

private enum class GateStep { Idle, Disclosure, Denied }

/**
 * @param required  permissions that must be granted for the action to run
 * @param optional  permissions requested together with [required] but not needed for the action
 *                  (e.g. notifications, or coarse location so the user may choose approximate)
 * @param disclosureTitle / [disclosureMessage] why the permissions are needed, shown before the system dialog
 * @param deniedMessage  shown, with a shortcut to settings, when the permission was permanently denied
 */
@Composable
fun rememberPermissionGate(
    required: List<String>,
    @StringRes disclosureTitle: Int,
    @StringRes disclosureMessage: Int,
    @StringRes deniedMessage: Int,
    optional: List<String> = emptyList(),
    onDenied: () -> Unit = {}
): PermissionGate {
    val context = LocalContext.current
    var step by remember { mutableStateOf(GateStep.Idle) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun allGranted() = required.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val action = pending
        pending = null
        if (allGranted()) {
            action?.invoke()
        } else {
            onDenied()
            val activity = context.findActivity()
            // No rationale after a denial means the system will not show the dialog again.
            val permanentlyDenied = activity != null && required.any {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
            if (permanentlyDenied) step = GateStep.Denied
        }
    }

    val gate = remember {
        PermissionGate(isGranted = ::allGranted) { onGranted ->
            if (allGranted()) onGranted() else {
                pending = onGranted
                step = GateStep.Disclosure
            }
        }
    }

    when (step) {
        GateStep.Disclosure -> PermissionDisclosureDialog(
            title = stringResource(disclosureTitle),
            message = stringResource(disclosureMessage),
            onContinue = {
                step = GateStep.Idle
                launcher.launch((required + optional).distinct().toTypedArray())
            },
            onDismiss = {
                step = GateStep.Idle
                pending = null
            }
        )
        GateStep.Denied -> PermissionDeniedDialog(
            message = stringResource(deniedMessage),
            onOpenSettings = {
                step = GateStep.Idle
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            onDismiss = { step = GateStep.Idle }
        )
        GateStep.Idle -> Unit
    }
    return gate
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
