package com.wandr.android

import android.app.Application
import com.wandr.android.watch.WatchWorkoutLink
import com.wandr.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class WandrApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin(
            appDeclaration = {
                androidLogger()
                androidContext(this@WandrApplication)
            }
        )

        WatchWorkoutLink.install(this)
    }
}
