package com.wandr.domain.repository

interface AppStorageRepository {
    /** Opens the local database (applying pending schema migrations) and verifies the file cache. */
    suspend fun prepare()
}
