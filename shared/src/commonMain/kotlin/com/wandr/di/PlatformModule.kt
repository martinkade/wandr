package com.wandr.di

import org.koin.core.module.Module

/** Platform-specific bindings (e.g. the Room database builder). */
expect val platformModule: Module
