package com.wandr.android.ui.activity

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import com.wandr.android.R
import com.wandr.android.ui.common.userMessage
import com.wandr.presentation.watch.WatchImportIntent
import com.wandr.presentation.watch.WatchImportViewModel
import org.koin.compose.koinInject

/**
 * Invisible host that imports workouts arriving from a watch while the main screen is shown: it reports the import
 * and opens the conflict wizard when a workout overlaps existing activities.
 */
@Composable
fun WatchImportHost(userId: String, teamId: String?, viewModel: WatchImportViewModel = koinInject()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(userId, teamId) { viewModel.processIntent(WatchImportIntent.Start(userId, teamId)) }

    val importedMessage = if (state.importedCount > 0) {
        pluralStringResource(R.plurals.watch_workouts_imported, state.importedCount, state.importedCount)
    } else null
    val errorText = state.error?.userMessage()
    LaunchedEffect(importedMessage, errorText) {
        (importedMessage ?: errorText)?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.processIntent(WatchImportIntent.ClearMessages)
        }
    }

    state.conflict?.let { conflict ->
        ActivityConflictSheet(
            conflict = conflict,
            isSaving = state.isSaving,
            onResolve = { viewModel.processIntent(WatchImportIntent.ResolveConflict(it)) },
            onDismiss = null // the workout would stay in the inbox without a decision
        )
    }
}
