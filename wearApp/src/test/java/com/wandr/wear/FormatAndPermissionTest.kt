package com.wandr.wear

import com.wandr.wear.format.WorkoutFormat
import com.wandr.wear.permissions.RequiredPermissions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FormatAndPermissionTest {
    @Test
    fun durationFormats() {
        assertEquals("0:00", WorkoutFormat.duration(-5))
        assertEquals("1:05", WorkoutFormat.duration(65_000))
        assertEquals("1:02:03", WorkoutFormat.duration(3_723_000))
    }

    @Test
    fun kilometersUseLocale() {
        assertEquals("1.50", WorkoutFormat.kilometers(1500.0, Locale.US))
        assertEquals("1,50", WorkoutFormat.kilometers(1500.0, Locale.GERMANY))
    }

    @Test
    fun permissionsDependOnSdk() {
        val api35 = RequiredPermissions.forSdk(35)
        assertTrue(RequiredPermissions.BODY_SENSORS in api35 && RequiredPermissions.POST_NOTIFICATIONS in api35)
        val api36 = RequiredPermissions.forSdk(36)
        assertTrue(RequiredPermissions.READ_HEART_RATE in api36)
        assertFalse(RequiredPermissions.BODY_SENSORS in api36)
        assertFalse(RequiredPermissions.POST_NOTIFICATIONS in RequiredPermissions.forSdk(32))
    }
}
