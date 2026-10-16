package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test fun `the system theme follows the phone`() {
        assertTrue(ThemeMode.System.isDark(systemDark = true))
        assertFalse(ThemeMode.System.isDark(systemDark = false))
    }

    @Test fun `light and dark ignore the phone`() {
        assertFalse(ThemeMode.Light.isDark(systemDark = true))
        assertTrue(ThemeMode.Dark.isDark(systemDark = false))
    }

    @Test fun `saved names read back, and unknown names fall back to the system theme`() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromName(it.name)) }
        assertEquals(ThemeMode.System, ThemeMode.fromName(null))
        assertEquals(ThemeMode.System, ThemeMode.fromName("Sepia"))
    }

    @Test fun `the brand palette in the phone's theme is the default`() {
        assertEquals(AppSettings(theme = ThemeMode.System, dynamicColor = false), AppSettings())
    }
}
