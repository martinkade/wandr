package com.wandr.data.local

import androidx.room3.RoomDatabase

expect class DatabaseBuilderFactory {
    fun create(): RoomDatabase.Builder<WandrDatabase>
}
