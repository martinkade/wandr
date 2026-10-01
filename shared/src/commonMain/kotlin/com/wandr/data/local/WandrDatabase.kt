package com.wandr.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.wandr.data.local.dao.ActivityDao
import com.wandr.data.local.dao.ChallengeDao
import com.wandr.data.local.dao.ChallengeParticipantDao
import com.wandr.data.local.dao.ProfileDao
import com.wandr.data.local.dao.TeamDao
import com.wandr.data.local.dao.TeamMemberDao
import com.wandr.data.local.entity.ActivityEntity
import com.wandr.data.local.entity.ChallengeEntity
import com.wandr.data.local.entity.ChallengeParticipantEntity
import com.wandr.data.local.entity.ProfileEntity
import com.wandr.data.local.entity.TeamEntity
import com.wandr.data.local.entity.TeamMemberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        ProfileEntity::class,
        TeamEntity::class,
        TeamMemberEntity::class,
        ChallengeEntity::class,
        ChallengeParticipantEntity::class,
        ActivityEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(WandrDatabaseConstructor::class)
abstract class WandrDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun teamDao(): TeamDao
    abstract fun teamMemberDao(): TeamMemberDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun challengeParticipantDao(): ChallengeParticipantDao
    abstract fun activityDao(): ActivityDao
}

// Room 3 / KMP Database Constructor expectation
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object WandrDatabaseConstructor : RoomDatabaseConstructor<WandrDatabase>

/**
 * Modern Room 3 Database Instantiator using BundledSQLiteDriver & Coroutines IO Dispatcher
 */
fun getWandrDatabase(builder: RoomDatabase.Builder<WandrDatabase>): WandrDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
