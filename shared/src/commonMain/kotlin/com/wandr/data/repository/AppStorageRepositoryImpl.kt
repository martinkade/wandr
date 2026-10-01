package com.wandr.data.repository

import androidx.room3.useReaderConnection
import com.wandr.data.local.WandrDatabase
import com.wandr.domain.repository.AppStorageRepository

class AppStorageRepositoryImpl(
    private val database: WandrDatabase
) : AppStorageRepository {

    override suspend fun prepare() {
        // Acquiring a connection forces Room to open the database and run pending migrations.
        database.useReaderConnection { }
    }
}
