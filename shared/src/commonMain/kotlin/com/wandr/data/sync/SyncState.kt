package com.wandr.data.sync

import com.wandr.domain.error.AppError

sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Success(val syncedCount: Int) : SyncState
    data class Error(val error: AppError) : SyncState
}
