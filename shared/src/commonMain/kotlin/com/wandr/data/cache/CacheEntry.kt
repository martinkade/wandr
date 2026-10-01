package com.wandr.data.cache

import kotlinx.serialization.Serializable

@Serializable
data class CacheEntry(
    val key: String,
    val sizeBytes: Long,
    var lastAccessedAt: Long
)
