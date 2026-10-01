package com.wandr.di

import com.wandr.data.cache.LruFileCache
import com.wandr.data.local.DatabaseBuilderFactory
import com.wandr.data.local.getWandrDatabase
import com.wandr.data.remote.KtorClientFactory
import com.wandr.data.remote.SupabaseClientFactory
import com.wandr.data.remote.SupabaseTokenProvider
import com.wandr.data.remote.TokenProvider
import com.wandr.data.repository.AuthRepositoryImpl
import com.wandr.data.repository.ChallengeRepositoryImpl
import com.wandr.data.repository.ProfileRepositoryImpl
import com.wandr.data.repository.TeamRepositoryImpl
import com.wandr.data.sync.SyncManager
import com.wandr.domain.repository.AuthRepository
import com.wandr.domain.repository.ChallengeRepository
import com.wandr.domain.repository.ProfileRepository
import com.wandr.domain.repository.TeamRepository
import org.koin.dsl.module

val dataModule = module {
    single { SupabaseClientFactory().create() }
    single<TokenProvider> { SupabaseTokenProvider(get()) }
    single { KtorClientFactory(get()).create() }
    single { getWandrDatabase(get<DatabaseBuilderFactory>().create()) }
    
    // LRU File Cache
    single { LruFileCache(initialMaxSizeBytes = 50 * 1024 * 1024L) }
    
    // DAOs
    single { get<com.wandr.data.local.WandrDatabase>().profileDao() }
    single { get<com.wandr.data.local.WandrDatabase>().teamDao() }
    single { get<com.wandr.data.local.WandrDatabase>().teamMemberDao() }
    single { get<com.wandr.data.local.WandrDatabase>().challengeDao() }
    single { get<com.wandr.data.local.WandrDatabase>().challengeParticipantDao() }
    single { get<com.wandr.data.local.WandrDatabase>().activityDao() }
    
    // Repositories & Sync
    single<com.wandr.domain.repository.AppStorageRepository> { com.wandr.data.repository.AppStorageRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<ProfileRepository> { ProfileRepositoryImpl(get(), get()) }
    single<TeamRepository> { TeamRepositoryImpl(get(), get(), get()) }
    single<ChallengeRepository> { ChallengeRepositoryImpl(get(), get(), get()) }
    single<com.wandr.domain.repository.ActivityRepository> { com.wandr.data.repository.ActivityRepositoryImpl(get(), get(), get()) }
    single { SyncManager(get(), get(), get(), get(), get()) }
}
