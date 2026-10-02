package com.wandr.wear.sync

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import com.wandr.wear.WandrWearApplication

/** Observes deletions of `/workouts/{id}` (the phone's acknowledgement) and clears the outbox, also when the app is closed. */
class WorkoutAckService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        val graph = (applicationContext as WandrWearApplication).graph
        for (event in events) {
            if (event.type != DataEvent.TYPE_DELETED) continue
            val path = event.dataItem.uri.path ?: continue
            if (!path.startsWith(DataLayerTransport.PATH_PREFIX)) continue
            graph.syncer.onAcknowledged(path.removePrefix(DataLayerTransport.PATH_PREFIX))
        }
    }
}
