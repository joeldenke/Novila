package se.novila.data

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.subtle.AesGcmJce
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.ThemeMode
import java.security.GeneralSecurityException
import java.security.SecureRandom

/**
 * A store whose file cannot be written, as when the Keystore entry is gone or the disk is full. The app cannot
 * fix that, so the write is left out and the store keeps serving the values it already has. Crashing there
 * would take the app down on every later save as well, and letting the failure end the store's consumer would
 * drop every write after it in silence.
 */
@RunWith(RobolectricTestRunner::class)
class PreferencesStoreWriteFailureTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()

    @After fun close() = stores.close()

    @Test fun `a write that cannot be sealed is left out and the store keeps working`() = runBlocking {
        val cipher = FlakyCipher()
        val store = SettingsStore(context, stores.scope, cipher)
        store.settings.await()

        cipher.failNextEncrypt = true
        store.setTheme(ThemeMode.Dark)

        // The store's consumer survived the failed write: the next one is applied, as any later save would be.
        store.setDynamicColor(true)
        store.settings.await { it.dynamicColor }

        val saved = store.settings.await()
        assertEquals("the write that could not be sealed must not land", ThemeMode.System, saved.theme)
        assertTrue("the write after the failed one must land", saved.dynamicColor)
    }

    /** A cipher that can be told to fail the next seal, as a Keystore that has become unavailable does. */
    private class FlakyCipher : DatastoreCipher {
        private val real = AesGcmJce(ByteArray(32).also { SecureRandom().nextBytes(it) })

        @Volatile var failNextEncrypt = false

        override fun aead(): Aead = object : Aead {
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray {
                if (failNextEncrypt) {
                    failNextEncrypt = false
                    throw GeneralSecurityException("the Keystore entry is gone")
                }
                return real.encrypt(plaintext, associatedData)
            }

            override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray =
                real.decrypt(ciphertext, associatedData)
        }
    }
}
