package com.wandr.di

import com.wandr.presentation.auth.LoginViewModel
import com.wandr.presentation.challenge.ChallengeViewModel
import com.wandr.presentation.profile.ProfileViewModel
import com.wandr.presentation.team.TeamViewModel
import org.koin.dsl.module

val viewModelModule = module {
    factory { com.wandr.presentation.startup.StartupViewModel(get()) }
    factory { com.wandr.presentation.main.MainViewModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { com.wandr.presentation.teamdetails.TeamDetailsViewModel(get(), get(), get(), get(), get(), get()) }
    factory { LoginViewModel(get(), get()) }
    factory { ProfileViewModel(get(), get(), get(), get(), get(), get()) }
    factory { TeamViewModel(get(), get(), get(), get(), get()) }
    factory { ChallengeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { com.wandr.presentation.activity.ActivityViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
}
