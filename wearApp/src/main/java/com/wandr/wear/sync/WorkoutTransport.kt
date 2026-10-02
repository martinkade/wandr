package com.wandr.wear.sync

/** Hands workouts to the phone. The Data Layer implementation is [DataLayerTransport]. */
interface WorkoutTransport {
    /** Ids whose data item currently exists, i.e. were sent and not yet acknowledged by the phone. */
    suspend fun sentIds(): Set<String>

    /** Puts the workout [json] into the transport; idempotent per [id]. Throws if it cannot be sent. */
    suspend fun send(id: String, json: String)
}
