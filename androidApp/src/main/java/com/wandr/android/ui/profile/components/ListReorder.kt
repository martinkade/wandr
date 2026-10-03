package com.wandr.android.ui.profile.components

/** A copy of the list with the item at [from] moved to [to] (both indices of the current list). */
internal fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
