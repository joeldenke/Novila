package se.novila.data

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LegacyPreferencesTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test fun `deleting all data removes the SharedPreferences left by an older version`() {
        val file = File(context.dataDir, "shared_prefs/health.xml")
        assertTrue(context.getSharedPreferences("health", Context.MODE_PRIVATE).edit().putString("day", "x").commit())
        assertTrue(file.isFile)

        deleteLegacyPreferences(context)

        assertFalse(file.isFile)
    }
}
