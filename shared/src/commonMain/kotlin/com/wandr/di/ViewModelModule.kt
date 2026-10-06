package com.wandr.di

import com.wandr.presentation.auth.LoginViewModel
import com.wandr.presentation.challenge.ChallengeViewModel
import com.wandr.presentation.profile.ProfileViewModel
import com.wandr.presentation.team.TeamViewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin qualifier of the process-wide [com.wandr.presentation.activity.ActivityViewModel] that holds the running recording.
 * Declared BEFORE [viewModelModule]: top-level values are initialized in order, and the module below reads it while it is built.
 */
val RecordingScope = named("recording")

val viewModelModule = module {
    factory { com.wandr.presentation.startup.StartupViewModel(get()) }
    factory { com.wandr.presentation.main.MainViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { com.wandr.presentation.teamdetails.TeamDetailsViewModel(get(), get(), get(), get(), get(), get()) }
    factory { LoginViewModel(get(), get()) }
    factory { ProfileViewModel(get(), get(), get(), get(), get(), get()) }
    factory { TeamViewModel(get(), get(), get(), get(), get(), get()) }
    factory { ChallengeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { com.wandr.presentation.activity.ActivityViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { com.wandr.presentation.watch.WatchImportViewModel(get(), get()) }
    factory { com.wandr.presentation.social.SocialViewModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { com.wandr.presentation.notifications.NotificationsViewModel(get(), get(), get()) }
    // The recording must outlive screens (rotation, leaving the app): the foreground service and the recording screen
    // both use this one instance. Browsing the feed uses the factory instances above.
    single(RecordingScope) { com.wandr.presentation.activity.ActivityViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
