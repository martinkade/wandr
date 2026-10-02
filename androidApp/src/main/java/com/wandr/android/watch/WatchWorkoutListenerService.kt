package com.wandr.android.watch

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import com.wandr.domain.watch.WatchWorkoutInbox
import org.koin.mp.KoinPlatform

/** Hands workouts that the watch sends to the inbox; importing happens in the app (it may need the user's decision). */
class WatchWorkoutListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        val inbox = KoinPlatform.getKoin().get<WatchWorkoutInbox>()
        events.filter { it.type == DataEvent.TYPE_CHANGED }.forEach { WatchWorkoutLink.offer(inbox, it.dataItem) }
    }
}
