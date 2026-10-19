package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import se.novila.model.BodyMeasurements
import se.novila.model.MeasurementEntry
import se.novila.model.MeasurementsCodec
import java.time.LocalDate

/**
 * The body measurements logged per day in Preferences DataStore (file `measurements`), on the device only: one key
 * per day, the day's measurements in [MeasurementsCodec]'s format. One instance for the whole app, so the
 * measurements screen sees what it saves through [entries] right away. A day measured as empty is removed, so the
 * history only holds days the user has logged.
 */
@SingleIn(AppScope::class)
class MeasurementsStore internal constructor(context: Context, scope: CoroutineScope, cipher: DatastoreCipher) {
    @Inject constructor(context: Context, cipher: DatastoreCipher) : this(context, storeScope(), cipher)

    private val store = PreferencesStore(context, NAME, scope, cipher)

    /** Every logged day, newest first. Null until the first read is done. */
    val entries: StateFlow<List<MeasurementEntry>?> = store.snapshots { prefs -> read(prefs) }

    /** Saves the measurements of [date] in the background, or forgets the day when [measurements] is empty. */
    fun save(date: LocalDate, measurements: BodyMeasurements) = store.edit { prefs ->
        val key = keyOf(date)
        if (measurements.isEmpty) prefs.remove(key) else prefs[key] = MeasurementsCodec.encode(measurements)
    }

    fun clear() = store.edit { it.clear() }

    /** One entry per day key, newest first. A key that is not a date and a line that does not decode are skipped. */
    private fun read(prefs: Preferences): List<MeasurementEntry> = prefs.asMap().mapNotNull { (key, value) ->
        val date = dateOf(key.name) ?: return@mapNotNull null
        MeasurementsCodec.decode(value as? String)
            .takeIf { !it.isEmpty }
            ?.let { MeasurementEntry(date, it) }
    }.sortedByDescending { it.date }

    private fun dateOf(name: String): LocalDate? = runCatching { LocalDate.parse(name) }.getOrNull()

    private fun keyOf(date: LocalDate) = stringPreferencesKey(date.toString())

    private companion object {
        const val NAME = "measurements"
    }
}
