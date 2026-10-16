package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import se.novila.coach.CoachPreferences
import se.novila.coach.CoachPreferencesCodec

/**
 * The coach's answers in Preferences DataStore (file `coach`, moved over from the SharedPreferences of the same name
 * on first read), on the device only. One instance for the whole app, so the Training tab sees what the coach screen
 * saves through [answers].
 */
@SingleIn(AppScope::class)
class CoachStore internal constructor(context: Context, scope: CoroutineScope) {
    @Inject constructor(context: Context) : this(context, storeScope())

    private val store = PreferencesStore(context, NAME, scope)

    /** The saved answers, kept in memory. Null until the first read is done. */
    val answers: StateFlow<SavedCoach?> = store.snapshots { SavedCoach(CoachPreferencesCodec.decode(it[KEY])) }

    /** Saves [preferences] in the background, in order with other saves. */
    fun save(preferences: CoachPreferences) = store.edit { it[KEY] = CoachPreferencesCodec.encode(preferences) }

    fun clear() = store.edit { it.remove(KEY) }

    private companion object {
        const val NAME = "coach"
        val KEY = stringPreferencesKey("preferences")
    }
}

/** The coach's answers as saved at one moment: null [preferences] until the user sets up the coach. */
data class SavedCoach(val preferences: CoachPreferences?)
