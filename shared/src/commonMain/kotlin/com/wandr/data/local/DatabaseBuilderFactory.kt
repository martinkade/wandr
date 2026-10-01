package com.wandr.data.local

import androidx.room.RoomDatabase

expect class DatabaseBuilderFactory {
    fun create(): RoomDatabase.Builder<WandrDatabase>
}
