package com.wandr.android.health

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wandr.domain.watch.WatchWorkoutInbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Reads new workouts from Health Connect whenever the app is resumed, as long as the connection is enabled (available
 * and permitted). Silent: the import reports its own result, and overlaps open the conflict wizard.
 */
@Composable
fun HealthAutoSync(inbox: WatchWorkoutInbox = koinInject()) {
    val context = LocalContext.current
    val importer = remember { HealthConnectImporter(context) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Off the main thread: the import reads and maps a month of workouts the first time.
                scope.launch(Dispatchers.Default) {
                    val recentlyRead = System.currentTimeMillis() - importer.lastReadAt < MIN_INTERVAL_MILLIS
                    if (!recentlyRead && importer.isAvailable) {
                        runCatching { if (importer.hasPermissions()) importer.readWorkouts().forEach(inbox::offer) }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/** Quick app switches should not hit Health Connect every time. */
private const val MIN_INTERVAL_MILLIS = 60_000L
