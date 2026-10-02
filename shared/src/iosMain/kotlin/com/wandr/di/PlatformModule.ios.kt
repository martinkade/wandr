package com.wandr.di

import com.wandr.data.fit.FitFileStorage
import com.wandr.data.local.DatabaseBuilderFactory
import okio.Path.Companion.toPath
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual val platformModule = module {
    single { DatabaseBuilderFactory() }
    // App-private folder; recorded FIT files stay on this device.
    single {
        val support = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .first() as String
        FitFileStorage(support.toPath() / "fit")
    }
}
