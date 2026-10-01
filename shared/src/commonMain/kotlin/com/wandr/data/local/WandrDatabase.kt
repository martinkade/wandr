package com.wandr.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
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

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object WandrDatabaseConstructor : RoomDatabaseConstructor<WandrDatabase>


fun getWandrDatabase(builder: RoomDatabase.Builder<WandrDatabase>): WandrDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
