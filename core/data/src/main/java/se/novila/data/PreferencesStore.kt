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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
 *
 * The snapshot is set from the value each write returns, in the same single consumer that runs the writes, instead of
 * from `dataStore.data` re-emitting. `dataStore.data` can drop a write's emission when it is collected while that
 * write is in flight (it starts its [kotlinx.coroutines.flow.dropWhile] from a version read before the write's new
 * value is published), which left a screen waiting for a save that had already happened. The first read is also
 * awaited before any write runs, so a write always reads what the last write left.
 */
internal class PreferencesStore(context: Context, name: String, private val scope: CoroutineScope) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        migrations = listOf(SharedPreferencesMigration(context, name)),
        scope = scope,
        produceFile = { context.preferencesDataStoreFile(name) },
    )

    /** The preferences as last read or written; null until the first read is done. */
    private val latest = MutableStateFlow<Preferences?>(null)
    private val writes = Channel<(MutablePreferences) -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            // An unreadable file reads as empty, as SharedPreferences did, instead of leaving the screens waiting.
            latest.value = try {
                dataStore.data.first()
            } catch (e: IOException) {
                emptyPreferences()
            }
            for (write in writes) {
                latest.value = dataStore.edit { write(it) }
            }
        }
    }

    /** The saved preferences as [T], or null until the first read is done. */
    fun <T : Any> snapshots(read: (Preferences) -> T): StateFlow<T?> =
        latest
            .map { preferences -> preferences?.let(read) }
            .stateIn(scope, SharingStarted.Eagerly, null)

    fun edit(write: (MutablePreferences) -> Unit) {
        writes.trySend(write)
    }
}
