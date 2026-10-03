package com.wandr.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InviteCodeTest {
    @Test
    fun bareCodeIsAccepted() {
        assertEquals("X7K9P2W1", InviteCode.parse("  X7K9P2W1 "))
        assertEquals("a1b2c3d4", InviteCode.parse("a1b2c3d4")) // server-generated codes are lower-case hex
    }

    @Test
    fun qrCodeUrlIsUnwrapped() {
        assertEquals("X7K9P2W1", InviteCode.parse("wandr://invite/X7K9P2W1"))
        assertEquals("X7K9P2W1", InviteCode.parse("WANDR://INVITE/X7K9P2W1/"))
    }

    @Test
    fun otherInputIsRejected() {
        assertNull(InviteCode.parse(""))
        assertNull(InviteCode.parse("abc"))
        assertNull(InviteCode.parse("https://example.com/invite/X7K9P2W1"))
        assertNull(InviteCode.parse("X7K9 P2W1"))
        assertNull(InviteCode.parse("wandr://invite/"))
    }
}
