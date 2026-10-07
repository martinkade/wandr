package com.wandr.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChallengeVisibilityTest {
    private val published = Challenge("c1", "Published", null, null, "individual", "distance", 1.0, false, 0L, 1L, "creator", 0L, 0L, isActive = true)
    private val draft = published.copy(id = "c2", title = "Draft", isActive = false)

    @Test
    fun publishedChallengesAreVisibleToEverybody() {
        assertTrue(published.isVisibleTo("anyone", isManager = false))
        assertTrue(published.isVisibleTo("creator", isManager = true))
    }

    @Test
    fun aDraftIsOnlyVisibleToItsCreatorWhoIsAManager() {
        assertTrue(draft.isVisibleTo("creator", isManager = true))
        assertFalse(draft.isVisibleTo("other-manager", isManager = true))
        assertFalse(draft.isVisibleTo("creator", isManager = false)) // e.g. the role was removed
        assertFalse(draft.isVisibleTo("anyone", isManager = false))
    }

    @Test
    fun filteringAListKeepsOnlyWhatTheUserMaySee() {
        val all = listOf(published, draft)
        assertEquals(listOf("c1"), all.filter { it.isVisibleTo("anyone", isManager = false) }.map { it.id })
        assertEquals(listOf("c1", "c2"), all.filter { it.isVisibleTo("creator", isManager = true) }.map { it.id })
    }
}
