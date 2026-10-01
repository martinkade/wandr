package com.wandr.di

import com.wandr.data.local.DatabaseBuilderFactory
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseBuilderFactory() }
}
