package com.wandr.android.ui.common

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class HeroTransitionTest {
    private val from =
        Rect(left = 100f, top = 400f, right = 160f, bottom = 460f) // a 60 px badge in the list
    private val to =
        Rect(left = 140f, top = 200f, right = 228f, bottom = 288f) // an 88 px badge on the page

    @Test
    fun theElementIsAtTheSourceWhenClosedAndAtTheTargetWhenOpen() {
        assertEquals(from, lerpRect(from, to, 0f))
        assertEquals(to, lerpRect(from, to, 1f))
    }

    @Test
    fun inBetweenItMovesAndGrowsInStep() {
        val half = lerpRect(from, to, 0.5f)
        assertEquals(120f, half.left, 0.001f)
        assertEquals(300f, half.top, 0.001f)
        assertEquals(74f, half.width, 0.001f) // between 60 and 88
    }

    @Test
    fun nothingFliesWithoutATapOrBeforeThePlacesAreKnown() {
        val state = HeroTransitionState()
        state.onProgress(0.5f)
        assertNull(state.currentRect("c1")) // no begin(), no places
        assertFalse(state.isFlying("c1"))
    }

    @Test
    fun progressIsKeptInsideZeroToOne() {
        val state = HeroTransitionState()
        state.onProgress(1.7f)
        assertEquals(1f, state.progress, 0f)
        state.onProgress(-0.2f)
        assertEquals(0f, state.progress, 0f)
    }
}
