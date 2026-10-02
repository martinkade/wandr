package com.wandr.wear.sync

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/** Wear OS Data Layer: one data item `/workouts/{id}` with the workout JSON under [KEY_JSON]. */
class DataLayerTransport(context: Context) : WorkoutTransport {
    private val client: DataClient = Wearable.getDataClient(context.applicationContext)

    override suspend fun sentIds(): Set<String> {
        val buffer = client.getDataItems(Uri.Builder().scheme("wear").path(PATH_PREFIX).build(), DataClient.FILTER_PREFIX).await()
        try {
            return buffer.mapNotNull { it.uri.lastPathSegment }.toSet()
        } finally {
            buffer.release()
        }
    }

    override suspend fun send(id: String, json: String) {
        val request = PutDataMapRequest.create(PATH_PREFIX + id).apply { dataMap.putString(KEY_JSON, json) }
            .asPutDataRequest()
            .setUrgent()
        client.putDataItem(request).await()
    }

    companion object {
        const val PATH_PREFIX = "/workouts/"
        const val KEY_JSON = "json"
    }
}
