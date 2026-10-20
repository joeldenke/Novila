package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * The stores as the app uses them on a phone: sealed by [KeystoreDatastoreCipher], whose keyset is built by
 * Tink. Tink registers nothing by itself, so until the cipher registers the AEAD configuration the keyset
 * cannot be built: "no key creator for this class was registered". Every DataStore write then throws inside
 * the store's scope, which has nothing to handle it, so the app crashes on the first save. Settings'
 * "Show the intro again" only flips `onboarded`, and on a phone that already finished the intro it is that
 * first write.
 */
@RunWith(RobolectricTestRunner::class)
class KeystoreDataStoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()
    private val file = context.preferencesDataStoreFile(SETTINGS)

    @After fun close() = stores.close()

    @Test fun `the app's cipher opens a keyset with the phone's Keystore`() {
        assertNotNull(KeystoreDatastoreCipher(context).aead())
    }

    @Test fun `showing the intro again is saved by the app's own cipher`() = runBlocking {
        // A phone that already finished the intro: the settings are on disk in the plaintext format the
        // current version still reads, and the row's tap is the first write it performs.
        file.parentFile?.mkdirs()
        file.outputStream().use { PreferencesFileSerializer.writeTo(preferencesOf(ONBOARDED to true), it) }

        val store = SettingsStore(context, stores.scope, KeystoreDatastoreCipher(context))
        store.settings.await { it.onboarded }

        store.setOnboarded(false)
        store.settings.await { !it.onboarded }

        // The save reached the disk, sealed: a reopened store reads it back and the file is not in the clear.
        stores.restart()
        val reopened = SettingsStore(context, stores.scope, KeystoreDatastoreCipher(context))
        assertFalse(reopened.settings.await().onboarded)
        assertFalse(file.readBytes().contains("onboarded".toByteArray()))
    }

    private fun ByteArray.contains(expected: ByteArray): Boolean =
        expected.isNotEmpty() && (0..size - expected.size).any { start ->
            expected.indices.all { this[start + it] == expected[it] }
        }

    private companion object {
        /** `SettingsStore`'s DataStore file name (its companion is private). */
        const val SETTINGS = "settings"
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }
}
