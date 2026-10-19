package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import se.novila.model.Reminder
import se.novila.model.ReminderSetting
import se.novila.model.Reminders
import java.time.LocalTime

/**
 * The user's reminders in Preferences DataStore (file `reminders`): for each, whether it is on and its time as minutes
 * after midnight. One instance for the whole app, so the Reminders screen and the reminders' own work read the same.
 */
@SingleIn(AppScope::class)
class RemindersStore internal constructor(context: Context, scope: CoroutineScope, cipher: DatastoreCipher) {
    @Inject constructor(context: Context, cipher: DatastoreCipher) : this(context, storeScope(), cipher)

    private val store = PreferencesStore(context, NAME, scope, cipher)

    /** The saved reminders, all off at their default times where nothing is saved. Null until the first read. */
    val reminders: StateFlow<Reminders?> = store.snapshots { prefs ->
        Reminder.entries.fold(Reminders()) { reminders, reminder ->
            val minute = prefs[minuteKey(reminder)]?.coerceIn(0, MINUTES_PER_DAY - 1)
            reminders.with(
                reminder,
                ReminderSetting(
                    on = prefs[onKey(reminder)] ?: false,
                    time = minute?.let { LocalTime.of(it / 60, it % 60) } ?: reminder.defaultTime,
                ),
            )
        }
    }

    fun save(reminder: Reminder, setting: ReminderSetting) = store.edit {
        it[onKey(reminder)] = setting.on
        it[minuteKey(reminder)] = setting.time.hour * 60 + setting.time.minute
    }

    /** Back to every reminder off at its default time, as when the user deletes all data. */
    fun clear() = store.edit { it.clear() }

    private companion object {
        const val NAME = "reminders"
        const val MINUTES_PER_DAY = 24 * 60

        fun onKey(reminder: Reminder) = booleanPreferencesKey("${reminder.name.lowercase()}_on")

        fun minuteKey(reminder: Reminder) = intPreferencesKey("${reminder.name.lowercase()}_minute")
    }
}
