package com.wandr.data.local

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

actual class DatabaseBuilderFactory(private val context: Context) {
    actual fun create(): RoomDatabase.Builder<WandrDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath("wandr.db")
        return Room.databaseBuilder<WandrDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )
    }
}
