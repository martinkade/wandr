package com.wandr.wear.model

/** Where a finished workout is on its way to the phone. */
enum class SendState {
    /** In the outbox, not yet put into the Data Layer. */
    PENDING,

    /** Put into the Data Layer, the phone has not acknowledged it yet. */
    SENT,

    /** The phone imported it and deleted the data item. */
    DELIVERED
}
