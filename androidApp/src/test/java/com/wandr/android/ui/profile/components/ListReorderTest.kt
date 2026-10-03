package com.wandr.android.ui.profile.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ListReorderTest {
    private val list = listOf("a", "b", "c", "d")

    @Test
    fun movesDownAndUp() {
        assertEquals(listOf("b", "a", "c", "d"), list.moved(0, 1))
        assertEquals(listOf("a", "c", "d", "b"), list.moved(1, 3))
        assertEquals(listOf("d", "a", "b", "c"), list.moved(3, 0))
    }

    @Test
    fun invalidMovesLeaveTheListAsItIs() {
        assertSame(list, list.moved(1, 1))
        assertSame(list, list.moved(-1, 2))
        assertSame(list, list.moved(0, 4))
    }
}
