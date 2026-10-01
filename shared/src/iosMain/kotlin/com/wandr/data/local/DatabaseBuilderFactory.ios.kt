package com.wandr.data.local

import androidx.room3.Room
import androidx.room3.RoomDatabase
import platform.Foundation.NSHomeDirectory

actual class DatabaseBuilderFactory {
    actual fun create(): RoomDatabase.Builder<WandrDatabase> {
        val dbFilePath = NSHomeDirectory() + "/wandr.db"
        return Room.databaseBuilder<WandrDatabase>(
            name = dbFilePath
        )
    }
}

