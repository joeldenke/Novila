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
import se.novila.exercise.ExerciseLog
import se.novila.exercise.ExerciseLogCodec
import se.novila.exercise.ExerciseResult

/**
 * The coach's answers and what the user did of each exercise in session mode ([ExerciseLog], for the progression)
 * in Preferences DataStore (file `coach`, moved over from the SharedPreferences of the same name on first read), on
 * the device only. One instance for the whole app, so the Training tab sees what the coach screen and session mode
 * save through [answers] and [progress].
 */
@SingleIn(AppScope::class)
class CoachStore internal constructor(context: Context, scope: CoroutineScope, cipher: DatastoreCipher) {
    @Inject constructor(context: Context, cipher: DatastoreCipher) : this(context, storeScope(), cipher)

    private val store = PreferencesStore(context, NAME, scope, cipher)

    /** The saved answers, kept in memory. Null until the first read is done. */
    val answers: StateFlow<SavedCoach?> = store.snapshots { SavedCoach(CoachPreferencesCodec.decode(it[KEY])) }

    /** Saves [preferences] in the background, in order with other saves. */
    fun save(preferences: CoachPreferences) = store.edit { it[KEY] = CoachPreferencesCodec.encode(preferences) }

    /** What the user did of each exercise lately, oldest first. Null until the first read is done. */
    val progress: StateFlow<ExerciseLog?> = store.snapshots { ExerciseLogCodec.decode(it[PROGRESS]) }

    /** Remembers what the user did in a finished session, in order with other saves. */
    fun recordSession(results: List<ExerciseResult>) {
        if (results.isEmpty()) return
        store.edit { prefs ->
            prefs[PROGRESS] = ExerciseLogCodec.encode(ExerciseLogCodec.decode(prefs[PROGRESS]).withSession(results))
        }
    }

    /** No answers and no exercise results. */
    fun clear() = store.edit {
        it.remove(KEY)
        it.remove(PROGRESS)
    }

    private companion object {
        const val NAME = "coach"
        val KEY = stringPreferencesKey("preferences")
        val PROGRESS = stringPreferencesKey("progress")
    }
}

/** The coach's answers as saved at one moment: null [preferences] until the user sets up the coach. */
data class SavedCoach(val preferences: CoachPreferences?)
