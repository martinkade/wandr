package com.wandr.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

/** Swift-friendly collection of a [StateFlow] on the main thread. Cancel the returned [Job] to stop. */
fun <T> StateFlow<T>.watch(onChange: (T) -> Unit): Job =
    mainScope.launch { collect { onChange(it) } }
