package se.novila

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutManagerCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppShortcutTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test fun `each shortcut opens the app with its own action`() {
        AppShortcut.entries.forEach { shortcut ->
            assertEquals(shortcut, AppShortcut.of(Intent(shortcut.action)))
        }
        assertNull(AppShortcut.of(Intent(Intent.ACTION_MAIN)))
        assertNull(AppShortcut.of(null))
    }

    @Test fun `the shortcuts are published, and open this build's main activity`() {
        AppShortcut.publish(context)

        val published = ShortcutManagerCompat.getDynamicShortcuts(context).sortedBy { it.id }
        assertEquals(listOf("log_water", "start_session"), published.map { it.id })
        assertEquals(listOf("Log a glass of water", "Start today's session"), published.map { it.longLabel.toString() })
        published.forEach { shortcut ->
            assertEquals(MainActivity::class.java.name, shortcut.intent.component?.className)
            assertEquals(context.packageName, shortcut.intent.component?.packageName)
        }
    }
}
