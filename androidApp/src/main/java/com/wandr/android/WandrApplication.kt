package com.wandr.android

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import com.wandr.android.push.PushNotifier
import com.wandr.android.watch.WatchWorkoutLink
import com.wandr.di.initKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class WandrApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Debug builds: disk and network access on the main thread shows up in the log (tag StrictMode).
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        }

        initKoin(
            appDeclaration = {
                androidLogger()
                androidContext(this@WandrApplication)
            }
        )

        PushNotifier.createChannels(this)

        // Firebase and the Wear OS Data Layer start their own services: not something the first frame should wait for.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            WatchWorkoutLink.install(this@WandrApplication)
            PushNotifier.fetchToken(this@WandrApplication)
        }
    }
}
