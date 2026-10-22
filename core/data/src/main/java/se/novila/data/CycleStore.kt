package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import se.novila.model.CycleLog
import se.novila.model.CycleLogCodec
import java.time.LocalDate

/**
 * *Follow your cycle* in Preferences DataStore (file `cycle`), on the device only and in a file of its own, since the
 * cycle is special category health data. Off by default. Turning it off deletes everything logged. One instance for
 * the whole app, so Training and My goals see the same [cycle].
 */
@SingleIn(AppScope::class)
class CycleStore internal constructor(context: Context, scope: CoroutineScope, cipher: DatastoreCipher) {
    @Inject constructor(context: Context, cipher: DatastoreCipher) : this(context, storeScope(), cipher)

    private val store = PreferencesStore(context, NAME, scope, cipher)

    /** Whether the user follows their cycle, and what they logged. Null until the first read is done. */
    val cycle: StateFlow<SavedCycle?> = store.snapshots { preferences ->
        val enabled = preferences[ENABLED] == true
        SavedCycle(enabled, if (enabled) CycleLogCodec.decode(preferences[LOG]) else CycleLog())
    }

    /** Turns *Follow your cycle* on, or off, which deletes what was logged. */
    fun setEnabled(enabled: Boolean) = store.edit {
        it[ENABLED] = enabled
        if (!enabled) it.remove(LOG)
    }

    /** Changes the log in the background, in order with other saves. Nothing is kept while the cycle is off. */
    fun update(change: (CycleLog) -> CycleLog) = store.edit {
        if (it[ENABLED] != true) return@edit
        val log = change(CycleLogCodec.decode(it[LOG]))
        if (log.days.isEmpty()) it.remove(LOG) else it[LOG] = CycleLogCodec.encode(log)
    }

    fun clear() = store.edit {
        it.remove(ENABLED)
        it.remove(LOG)
    }

    private companion object {
        const val NAME = "cycle"
        val ENABLED = booleanPreferencesKey("enabled")
        val LOG = stringPreferencesKey("log")
    }
}

/** *Follow your cycle* as saved at one moment: [enabled] or not, and the [log], empty while it is off. */
data class SavedCycle(val enabled: Boolean = false, val log: CycleLog = CycleLog()) {
    /** The cycle's period days, or none while it is off. */
    val periodDays: Set<LocalDate> get() = if (enabled) log.periodDays else emptySet()

    /** The user chose a lighter session for [date]. */
    fun lighter(date: LocalDate): Boolean = enabled && log.lighter(date)
}
