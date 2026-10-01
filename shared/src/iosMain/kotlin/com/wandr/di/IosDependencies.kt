package com.wandr.di

import com.wandr.presentation.activity.ActivityViewModel
import com.wandr.presentation.auth.LoginViewModel
import com.wandr.presentation.dashboard.DashboardViewModel
import com.wandr.presentation.profile.ProfileViewModel
import com.wandr.presentation.startup.StartupViewModel
import org.koin.mp.KoinPlatform

/** Entry points for Swift, which cannot use Koin's reified `get<T>()`. */
object IosDependencies {
    fun startupViewModel(): StartupViewModel = KoinPlatform.getKoin().get()
    fun loginViewModel(): LoginViewModel = KoinPlatform.getKoin().get()
    fun dashboardViewModel(): DashboardViewModel = KoinPlatform.getKoin().get()
    fun activityViewModel(): ActivityViewModel = KoinPlatform.getKoin().get()
    fun profileViewModel(): ProfileViewModel = KoinPlatform.getKoin().get()
}
