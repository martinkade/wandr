package com.wandr.wear

import android.content.Context
import com.wandr.wear.sync.DataLayerTransport
import com.wandr.wear.sync.OutboxSyncer
import com.wandr.wear.sync.WorkoutOutbox
import com.wandr.wear.tracking.ExerciseTracker
import java.io.File

/** Hand-wired singletons (the watch app is small; no DI framework). */
class AppGraph(context: Context) {
    val outbox = WorkoutOutbox(File(context.filesDir, "outbox"))
    val syncer = OutboxSyncer(outbox, DataLayerTransport(context))
    val tracker = ExerciseTracker(context)
}
