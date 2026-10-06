package com.wandr.di

import com.wandr.presentation.activity.ActivityViewModel
import com.wandr.presentation.auth.LoginViewModel
import com.wandr.presentation.main.MainViewModel
import com.wandr.presentation.profile.ProfileViewModel
import com.wandr.presentation.startup.StartupViewModel
import com.wandr.presentation.team.TeamViewModel
import com.wandr.presentation.teamdetails.TeamDetailsViewModel
import com.wandr.domain.watch.WatchWorkoutInbox
import com.wandr.presentation.watch.WatchImportViewModel
import com.wandr.domain.push.PushTokenManager
import com.wandr.presentation.notifications.NotificationsViewModel
import com.wandr.presentation.social.SocialViewModel
import org.koin.mp.KoinPlatform

/** Entry points for Swift, which cannot use Koin's reified `get<T>()`. */
object IosDependencies {
    fun startupViewModel(): StartupViewModel = KoinPlatform.getKoin().get()
    fun loginViewModel(): LoginViewModel = KoinPlatform.getKoin().get()
    fun mainViewModel(): MainViewModel = KoinPlatform.getKoin().get()
    fun activityViewModel(): ActivityViewModel = KoinPlatform.getKoin().get()
    /** The process-wide instance that holds the running recording. */
    fun recordingViewModel(): ActivityViewModel = KoinPlatform.getKoin().get(RecordingScope)
    fun profileViewModel(): ProfileViewModel = KoinPlatform.getKoin().get()
    fun teamViewModel(): TeamViewModel = KoinPlatform.getKoin().get()
    fun teamDetailsViewModel(): TeamDetailsViewModel = KoinPlatform.getKoin().get()
    fun watchImportViewModel(): WatchImportViewModel = KoinPlatform.getKoin().get()
    fun watchWorkoutInbox(): WatchWorkoutInbox = KoinPlatform.getKoin().get()
    fun socialViewModel(): SocialViewModel = KoinPlatform.getKoin().get()
    fun notificationsViewModel(): NotificationsViewModel = KoinPlatform.getKoin().get()
    fun pushTokenManager(): PushTokenManager = KoinPlatform.getKoin().get()
}
