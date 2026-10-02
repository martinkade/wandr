package com.wandr.di

import com.wandr.data.fit.FitFileStorage
import com.wandr.data.local.DatabaseBuilderFactory
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseBuilderFactory(androidContext()) }
    // App-private folder; recorded FIT files stay on this device.
    single { FitFileStorage(androidContext().filesDir.absolutePath.toPath() / "fit") }
}
