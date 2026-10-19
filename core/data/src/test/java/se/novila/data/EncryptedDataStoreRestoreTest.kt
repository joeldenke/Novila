package se.novila.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.backup.BackupActionInputKeys
import androidx.test.backup.BackupActionValues
import androidx.test.backup.BackupDeviceActionArgs
import androidx.test.backup.actions.AssertStorageAction
import androidx.test.backup.actions.PopulateStorageAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.model.ThemeMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Restores Novila's real DataStore file with the device-side actions from `androidx.test.backup` (the same actions
 * the automated backup and restore suite uses, see docs/backup.md) and checks what a restore now means:
 *
 * - A backup taken by an older app version holds a plaintext file; the restored store reads it and re-seals it.
 * - The restored bytes are the ones the rules carry, verified with the library's assertion action.
 *
 * The platform Auto Backup transport itself can only run on a device or emulator; this covers the part that runs
 * in CI.
 */
@RunWith(RobolectricTestRunner::class)
class EncryptedDataStoreRestoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val stores = StoreScope()

    @After fun close() = stores.close()

    @Test fun `a restored plaintext backup is migrated and re-sealed`() = runBlocking {
        val plaintext = plaintextSettings(stringPreferencesKey("theme") to ThemeMode.Dark.name)
        val payload =
            mapOf(
                BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_FILES,
                BackupActionInputKeys.PATH to DATA_STORE_FILE,
                BackupActionInputKeys.IS_BINARY to "true",
                BackupActionInputKeys.VALUE to Base64.encodeToString(plaintext, Base64.NO_WRAP),
            )

        // A wipe, as after a factory reset: the backup is all that is left.
        val restored = PopulateStorageAction().execute(context, BackupDeviceActionArgs(payload))
        assertTrue(restored.errorMessage.orEmpty(), restored.isSuccess)

        val expected = Base64.encodeToString(plaintext, Base64.NO_WRAP)
        val verified =
            AssertStorageAction()
                .execute(context, BackupDeviceActionArgs(payload + (BackupActionInputKeys.EXPECTED to expected)))
        assertTrue(verified.errorMessage.orEmpty(), verified.isSuccess)

        val store = SettingsStore(context, stores.scope, stores.cipher)
        assertEquals(ThemeMode.Dark, store.settings.await().theme)
        // The next write seals the file: a backup taken now carries no plaintext.
        store.setTheme(ThemeMode.Light)
        assertFalse(file().awaitWritten().contains(ThemeMode.Light.name.toByteArray()))
    }

    /** A preferences file as Preferences DataStore wrote it before the move to encryption. */
    private suspend fun plaintextSettings(vararg pairs: Preferences.Pair<*>): ByteArray {
        val out = ByteArrayOutputStream()
        PreferencesFileSerializer.writeTo(preferencesOf(*pairs), out)
        return out.toByteArray()
    }

    private fun file() = File(context.filesDir, DATA_STORE_FILE)

    private suspend fun File.awaitWritten(): ByteArray {
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30)
        while (System.currentTimeMillis() < deadline) {
            takeIf { it.isFile }?.readBytes()?.takeIf { it.isNotEmpty() }?.let { return it }
            delay(25)
        }
        error("$this was never written")
    }

    private fun ByteArray.contains(expected: ByteArray): Boolean =
        expected.isNotEmpty() && (0..size - expected.size).any { start ->
            expected.indices.all { this[start + it] == expected[it] }
        }

    private companion object {
        /** Where Preferences DataStore keeps `SettingsStore` (`files/datastore/<name>.preferences_pb`). */
        const val DATA_STORE_FILE = "datastore/settings.preferences_pb"
    }
}
