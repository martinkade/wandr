package com.wandr.android.push

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.wandr.android.R
import com.wandr.android.ui.permission.rememberPermissionGate

private const val PREFS = "push_prefs"
private const val KEY_ASKED = "notifications_asked"

/**
 * Asks once (Android 13+) for the notification permission, preceded by the disclosure dialog. Renders nothing itself;
 * does nothing when already granted, already asked, or below API 33.
 */
@Composable
fun PushPermissionPrompt() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val gate = rememberPermissionGate(
        required = listOf(Manifest.permission.POST_NOTIFICATIONS),
        disclosureTitle = R.string.permission_push_title,
        disclosureMessage = R.string.permission_push_message,
        deniedMessage = R.string.permission_push_denied
    )
    LaunchedEffect(Unit) {
        if (gate.granted || prefs.getBoolean(KEY_ASKED, false)) return@LaunchedEffect
        prefs.edit { putBoolean(KEY_ASKED, true) }
        gate.request { }
    }
}
