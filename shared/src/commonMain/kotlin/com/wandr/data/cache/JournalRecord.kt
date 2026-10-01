package com.wandr.data.cache

import kotlinx.serialization.Serializable

@Serializable
enum class JournalAction {
    READ,
    WRITE,
    REMOVE,
    EVICT
}

@Serializable
data class JournalRecord(
    val action: JournalAction,
    val key: String,
    val timestamp: Long,
    val sizeBytes: Long = 0L
)
