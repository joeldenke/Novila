package se.novila.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

/** The scope a store reads and writes in: the whole app's lifetime, off the main thread. */
internal fun storeScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * One Preferences DataStore file, [name], that the SharedPreferences of the same name are moved into on first read
 * (then deleted). Only one may exist per file in a process, so each store is a singleton in `AppScope`.
 *
 * Reads are kept in memory as a [StateFlow], so a screen opened after the first read gets its data right away.
 * Writes are queued and applied one at a time in [scope], like `SharedPreferences.apply()`: in order, and finished
 * even when the ViewModel that asked for them is cleared.
 */
internal class PreferencesStore(context: Context, name: String, private val scope: CoroutineScope) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        migrations = listOf(SharedPreferencesMigration(context, name)),
        scope = scope,
        produceFile = { context.preferencesDataStoreFile(name) },
    )
    private val writes = Channel<(MutablePreferences) -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (write in writes) dataStore.edit { write(it) }
        }
    }

    /** The saved preferences as [T], or null until the first read is done. */
    fun <T : Any> snapshots(read: (Preferences) -> T): StateFlow<T?> =
        dataStore.data
            // An unreadable file reads as empty, as SharedPreferences did, instead of leaving the screens waiting.
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map(read)
            .stateIn(scope, SharingStarted.Eagerly, null)

    fun edit(write: (MutablePreferences) -> Unit) {
        writes.trySend(write)
    }
}
