package se.novila.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import se.novila.model.DayEntry
import se.novila.model.DayEntryCodec
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.historyOf
import java.time.LocalDate

/**
 * Local storage: one encoded [DayEntry] per date in Preferences DataStore (file `health`, moved over from the
 * SharedPreferences of the same name on first read). One instance for the whole app, so every screen's ViewModel
 * sees a save through [days].
 */
@SingleIn(AppScope::class)
class HealthStore internal constructor(context: Context, scope: CoroutineScope) {
    @Inject constructor(context: Context) : this(context, storeScope())

    private val store = PreferencesStore(context, NAME, scope)

    /** Every saved day, read from disk once and then kept in memory. Null until the first read is done. */
    val days: StateFlow<SavedDays?> = store.snapshots { SavedDays(it) }

    /** The history over each range in [ranges] up to each date in [dates], read again whenever a day is saved. */
    fun histories(dates: Flow<LocalDate>, ranges: Flow<HistoryRange>): Flow<History> =
        combine(dates, ranges, days.filterNotNull()) { date, range, saved -> saved.history(range, date) }

    /** The history over [range] up to [until] as saved right now, or null before the first read. */
    fun history(range: HistoryRange, until: LocalDate): History? = days.value?.history(range, until)

    /**
     * Changes the day at [date] and saves it. Returns at once; saves are written in order, each reading the day as
     * the previous save left it, and finish even if the caller's scope is cancelled.
     */
    fun update(date: LocalDate, change: (DayEntry) -> DayEntry) = store.edit { prefs ->
        val key = keyOf(date)
        prefs[key] = DayEntryCodec.encode(change(DayEntryCodec.decode(date, prefs[key])))
    }

    /** Forgets every saved day, in order with other saves. */
    fun clear() = store.edit { it.clear() }

    private companion object {
        const val NAME = "health"

        fun keyOf(date: LocalDate) = stringPreferencesKey(date.toString())
    }

    /**
     * The days as saved at one moment, each shown through [shown] (as saved unless [showing] gives another view, such
     * as a watch's night filling in sleep that wasn't logged).
     */
    class SavedDays internal constructor(
        private val prefs: Preferences,
        private val shown: (DayEntry) -> DayEntry = { it },
    ) {
        fun day(date: LocalDate): DayEntry = shown(DayEntryCodec.decode(date, prefs[keyOf(date)]))

        /** The same days, each shown through [change] as well. Only for reading: saves always start from disk. */
        fun showing(change: (DayEntry) -> DayEntry): SavedDays = SavedDays(prefs) { change(shown(it)) }

        /** [count] days up to [today], newest first. */
        fun lastDays(count: Int, today: LocalDate): List<DayEntry> =
            (0 until count).map { day(today.minusDays(it.toLong())) }

        /** Today and the six days before it. */
        fun week(today: LocalDate): Week = lastDays(count = 7, today = today).let { Week(it.first(), it.drop(1)) }

        /** The days over [range] up to [until], and as many before them to compare with. */
        fun history(range: HistoryRange, until: LocalDate): History = historyOf(range, until, ::day)
    }
}

/** [today] and the days before it, newest first. */
data class Week(val today: DayEntry, val history: List<DayEntry>)
