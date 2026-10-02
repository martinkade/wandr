package com.wandr.android.watch

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.wandr.domain.watch.WatchWorkoutInbox
import org.koin.mp.KoinPlatform

/**
 * Connects the Wear OS Data Layer with the [WatchWorkoutInbox]. The watch puts a workout as data item
 * `/workouts/{id}` (key `json`); deleting that item is the acknowledgement that the phone has handled it.
 */
object WatchWorkoutLink {
    private const val PATH_PREFIX = "/workouts/"
    private const val KEY_JSON = "json"

    /** Acknowledges handled workouts to the watch, and picks up workouts that arrived while the app was not running. */
    fun install(context: Context) {
        val appContext = context.applicationContext
        val inbox = KoinPlatform.getKoin().get<WatchWorkoutInbox>()
        val dataClient = Wearable.getDataClient(appContext)

        inbox.onHandled = { id ->
            // No host in the URI: the item is deleted regardless of the node it came from.
            dataClient.deleteDataItems(Uri.Builder().scheme("wear").path("$PATH_PREFIX$id").build())
        }

        // Without Google Play services / a paired watch the task simply fails; that is not an error for the app.
        dataClient.getDataItems(Uri.Builder().scheme("wear").path(PATH_PREFIX.trimEnd('/')).build(), 1 /* FILTER_PREFIX */)
            .addOnSuccessListener { items ->
                items.use { buffer -> buffer.forEach { offer(inbox, it) } }
            }
    }

    internal fun offer(inbox: WatchWorkoutInbox, item: com.google.android.gms.wearable.DataItem) {
        if (item.uri.path?.startsWith(PATH_PREFIX) != true) return
        val json = DataMapItem.fromDataItem(item).dataMap.getString(KEY_JSON) ?: return
        inbox.offerJson(json)
    }
}
