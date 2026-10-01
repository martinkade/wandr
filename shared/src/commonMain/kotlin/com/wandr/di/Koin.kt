package com.wandr.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}, extraModules: List<Module> = emptyList()) =
    startKoin {
        appDeclaration()
        modules(
            listOf(
                dataModule,
                domainModule,
                viewModelModule
            ) + extraModules
        )
    }

// Helper for iOS initialization
fun initKoin() = initKoin(appDeclaration = {}, extraModules = emptyList())
