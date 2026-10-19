package se.novila.backup

import android.content.Context
import android.util.Base64
import androidx.test.backup.BackupActionInputKeys
import androidx.test.backup.BackupActionValues
import androidx.test.backup.BackupDeviceActionArgs
import androidx.test.backup.actions.AssertStorageAction
import androidx.test.backup.actions.PopulateStorageAction
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import se.novila.data.SettingsStore
import se.novila.model.ThemeMode
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Restores Novila's real DataStore file with the device-side actions from
 * `androidx.test.backup` (the same actions the automated backup and restore suite uses, see
 * docs/backup.md) and verifies the restored bytes with that library's assertion action.
 *
 * The platform Auto Backup transport itself can only run on a device or emulator; this covers the
 * part that runs in CI: the file is the one the rules include, and the restored file is intact.
 */
@RunWith(RobolectricTestRunner::class)
class BackupRestoreTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test fun `settings DataStore survives a restore`() = runBlocking {
        SettingsStore(context).apply {
            setTheme(ThemeMode.Dark)
            settings.await { it.theme == ThemeMode.Dark }
        }

        val file = File(context.filesDir, DATA_STORE_FILE)
        assertTrue("SettingsStore must write $DATA_STORE_FILE", file.isFile)
        val backedUp = file.readBytes()

        // A wipe, as after a factory reset: the backup is all that is left.
        assertTrue(file.delete())

        val payload =
            mapOf(
                BackupActionInputKeys.STORAGE_TYPE to BackupActionValues.STORAGE_TYPE_FILES,
                BackupActionInputKeys.PATH to DATA_STORE_FILE,
                BackupActionInputKeys.IS_BINARY to "true",
                BackupActionInputKeys.VALUE to Base64.encodeToString(backedUp, Base64.NO_WRAP),
            )
        val restored = PopulateStorageAction().execute(context, BackupDeviceActionArgs(payload))
        assertTrue(restored.errorMessage.orEmpty(), restored.isSuccess)

        val expected = Base64.encodeToString(backedUp, Base64.NO_WRAP)
        val verified =
            AssertStorageAction()
                .execute(
                    context,
                    BackupDeviceActionArgs(payload + (BackupActionInputKeys.EXPECTED to expected)),
                )
        assertTrue(verified.errorMessage.orEmpty(), verified.isSuccess)
        assertArrayEquals(backedUp, file.readBytes())
    }

    private suspend fun <T : Any> StateFlow<T?>.await(predicate: (T) -> Boolean): T =
        withTimeout(TimeUnit.SECONDS.toMillis(30)) { filterNotNull().first(predicate) }

    private companion object {
        /** Where Preferences DataStore keeps `SettingsStore` (`files/datastore/<name>.preferences_pb`). */
        const val DATA_STORE_FILE = "datastore/settings.preferences_pb"
    }
}
