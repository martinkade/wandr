package com.wandr.presentation.main

data class MainState(
    val userId: String? = null,
    val teamId: String? = null,
    /** True when the profile's system role is MANAGER; unlocks the "Teams" tab. */
    val isManager: Boolean = false,
    val isLoading: Boolean = true
)
