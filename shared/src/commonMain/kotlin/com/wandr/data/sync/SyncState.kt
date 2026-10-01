package com.wandr.data.sync

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Success(val syncedCount: Int) : SyncState
    data class Error(val message: String) : SyncState
}
