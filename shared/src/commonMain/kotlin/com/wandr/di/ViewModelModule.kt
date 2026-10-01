package com.wandr.di

import com.wandr.presentation.auth.LoginViewModel
import com.wandr.presentation.profile.ProfileViewModel
import com.wandr.presentation.team.TeamViewModel
import org.koin.dsl.module

val viewModelModule = module {
    factory { LoginViewModel(get(), get()) }
    factory { ProfileViewModel(get(), get(), get()) }
    factory { TeamViewModel(get(), get(), get(), get()) }
}
