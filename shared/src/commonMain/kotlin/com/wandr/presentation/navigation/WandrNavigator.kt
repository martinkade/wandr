package com.wandr.presentation.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey

class WandrNavigator(
    val backStack: SnapshotStateList<NavKey>
) {
    fun navigateTo(route: Route) {
        backStack.add(route)
    }

    fun popBackStack(): Boolean {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            return true
        }
        return false
    }

    fun replaceRoot(route: Route) {
        backStack.clear()
        backStack.add(route)
    }
}
