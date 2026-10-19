package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.AppSettings
import se.novila.model.ThemeMode
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * How the stores' files look and behave on disk: sealed with the cipher, still readable when an older version
 * wrote them in plaintext, and replaced with empty data when they cannot be opened at all.
 */
@RunWith(RobolectricTestRunner::class)
class EncryptedDataStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()
    private val file = context.preferencesDataStoreFile(SETTINGS)

    @After fun close() = stores.close()

    @Test fun `a saved file does not carry the values in the clear`() = runBlocking {
        SettingsStore(context, stores.scope, stores.cipher).setTheme(ThemeMode.Dark)

        val bytes = file.awaitWritten()
        assertEquals("the file must start with the encrypted format's header", HEADER, bytes.first())
        assertFalse("the theme must not be readable in the file", bytes.contains("Dark".toByteArray()))
    }

    @Test fun `a plaintext file from an older version is read and then sealed`() = runBlocking {
        writePlaintext(preferencesOf(stringPreferencesKey("theme") to ThemeMode.Dark.name))

        val store = SettingsStore(context, stores.scope, stores.cipher)
        assertEquals(ThemeMode.Dark, store.settings.await().theme)

        // The next write stores the file sealed, and a reopened store still reads it back.
        store.setTheme(ThemeMode.Light)
        stores.restart()
        val reopened = SettingsStore(context, stores.scope, stores.cipher)
        assertEquals(ThemeMode.Light, reopened.settings.await().theme)
        assertFalse(file.awaitWritten().contains(ThemeMode.Light.name.toByteArray()))
    }

    @Test fun `a file sealed with another key reads as empty instead of failing`() = runBlocking {
        SettingsStore(context, stores.scope, stores.cipher).setTheme(ThemeMode.Dark)
        file.awaitWritten()
        stores.restart()

        // A cipher whose key does not match the one the file was sealed with (as after a restore onto a new phone).
        val other = SettingsStore(context, stores.scope, testCipher())
        assertEquals(AppSettings(), other.settings.await())
    }

    @Test fun `a truncated file reads as empty instead of failing`() = runBlocking {
        SettingsStore(context, stores.scope, stores.cipher).setTheme(ThemeMode.Dark)
        val bytes = file.awaitWritten()
        file.writeBytes(bytes.copyOfRange(0, bytes.size / 2))
        stores.restart()

        assertEquals(AppSettings(), SettingsStore(context, stores.scope, stores.cipher).settings.await())
    }

    /** Writes a legacy plaintext preferences file, as Preferences DataStore did before the move to encryption. */
    private suspend fun writePlaintext(preferences: Preferences) {
        file.parentFile?.mkdirs()
        file.outputStream().use { PreferencesFileSerializer.writeTo(preferences, it) }
    }

    private suspend fun File.awaitWritten(): ByteArray {
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30)
        while (System.currentTimeMillis() < deadline) {
            takeIf { it.isFile && it.length() > 0 }?.readBytes()?.let { return it }
            delay(25)
        }
        error("$this was never written")
    }

    private fun ByteArray.contains(expected: ByteArray): Boolean =
        expected.isNotEmpty() && (0..size - expected.size).any { start ->
            expected.indices.all { this[start + it] == expected[it] }
        }

    private companion object {
        /** `SettingsStore`'s DataStore file name (its companion is private). */
        const val SETTINGS = "settings"
        const val HEADER = 0x4E.toByte()
    }
}
