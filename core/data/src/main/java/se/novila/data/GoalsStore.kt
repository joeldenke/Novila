package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import se.novila.model.Goals

/**
 * The user's daily goals in Preferences DataStore (file `goals`), on the device only. One instance for the whole
 * app, so Today, Sleep and Nutrition measure against a new goal as soon as the goals screen saves it.
 */
@SingleIn(AppScope::class)
class GoalsStore internal constructor(context: Context, scope: CoroutineScope) {
    @Inject constructor(context: Context) : this(context, storeScope())

    private val store = PreferencesStore(context, NAME, scope)

    /** The saved goals, the defaults where nothing is saved. Null until the first read is done. */
    val goals: StateFlow<Goals?> = store.snapshots {
        val defaults = Goals()
        Goals(
            steps = it[STEPS] ?: defaults.steps,
            sleepHours = it[SLEEP_HOURS] ?: defaults.sleepHours,
            waterGlasses = it[WATER_GLASSES] ?: defaults.waterGlasses,
        ).clamped()
    }

    fun save(goals: Goals) = store.edit {
        it[STEPS] = goals.steps
        it[SLEEP_HOURS] = goals.sleepHours
        it[WATER_GLASSES] = goals.waterGlasses
    }

    /** Back to the defaults. */
    fun clear() = store.edit { it.clear() }

    private companion object {
        const val NAME = "goals"
        val STEPS = intPreferencesKey("steps")
        val SLEEP_HOURS = floatPreferencesKey("sleep_hours")
        val WATER_GLASSES = intPreferencesKey("water_glasses")
    }
}
