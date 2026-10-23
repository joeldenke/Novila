package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import se.novila.sleep.SleepCoachPreferences
import se.novila.sleep.SleepCoachPreferencesCodec

/**
 * The sleep coach's answers in Preferences DataStore (file `sleep_coach`), on the device only. One instance for the
 * whole app, so the Sleep tab sees what the coach screen saves through [answers].
 */
@SingleIn(AppScope::class)
class SleepCoachStore internal constructor(context: Context, scope: CoroutineScope, cipher: DatastoreCipher) {
    @Inject constructor(context: Context, cipher: DatastoreCipher) : this(context, storeScope(), cipher)

    internal val store = PreferencesStore(context, NAME, scope, cipher)

    /** The saved answers, kept in memory. Null until the first read is done. */
    val answers: StateFlow<SavedSleepCoach?> =
        store.snapshots { SavedSleepCoach(SleepCoachPreferencesCodec.decode(it[KEY])) }

    /** Saves [preferences] in the background, in order with other saves. */
    fun save(
        preferences: SleepCoachPreferences,
    ) = store.edit { it[KEY] = SleepCoachPreferencesCodec.encode(preferences) }

    fun clear() = store.edit { it.remove(KEY) }

    private companion object {
        const val NAME = "sleep_coach"
        val KEY = stringPreferencesKey("preferences")
    }
}

/** The sleep coach's answers as saved at one moment: null [preferences] until the user sets up the coach. */
data class SavedSleepCoach(val preferences: SleepCoachPreferences?)
