package se.novila.data

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import se.novila.coach.CoachPreferences
import se.novila.coach.CoachPreferencesCodec

/**
 * The coach's answers in SharedPreferences, on the device only. Null until the user sets up the coach. One instance
 * for the whole app, so the Training tab sees what the coach screen saves through [changes].
 */
@SingleIn(AppScope::class)
@Inject
class CoachStore(context: Context) {
    private val prefs = context.getSharedPreferences("coach", Context.MODE_PRIVATE)
    private val revision = MutableStateFlow(0)

    /** Counts saves and resets. */
    val changes: StateFlow<Int> = revision.asStateFlow()

    fun load(): CoachPreferences? = CoachPreferencesCodec.decode(prefs.getString(KEY, null))

    fun save(preferences: CoachPreferences) {
        prefs.edit().putString(KEY, CoachPreferencesCodec.encode(preferences)).apply()
        revision.update { it + 1 }
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
        revision.update { it + 1 }
    }

    private companion object {
        const val KEY = "preferences"
    }
}
