package se.novila.data

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import se.novila.model.DayEntry
import se.novila.model.DayEntryCodec
import java.time.LocalDate

/**
 * Local storage: one encoded [DayEntry] per date in SharedPreferences. One instance for the whole app, so every
 * screen's ViewModel sees a save through [changes].
 */
@SingleIn(AppScope::class)
@Inject
class HealthStore(context: Context) {
    private val prefs = context.getSharedPreferences("health", Context.MODE_PRIVATE)
    private val revision = MutableStateFlow(0)

    /** Counts saves. ViewModels read again when it changes. */
    val changes: StateFlow<Int> = revision.asStateFlow()

    fun load(date: LocalDate): DayEntry =
        DayEntryCodec.decode(date, prefs.getString(date.toString(), null))

    fun save(entry: DayEntry) {
        prefs.edit().putString(entry.date.toString(), DayEntryCodec.encode(entry)).apply()
        revision.update { it + 1 }
    }

    /** Changes the day at [date] and saves it. */
    fun update(date: LocalDate, change: (DayEntry) -> DayEntry) = save(change(load(date)))

    fun lastDays(count: Int, today: LocalDate): List<DayEntry> =
        (0 until count).map { load(today.minusDays(it.toLong())) }

    /** Today and the six days before it. */
    fun week(today: LocalDate): Week = lastDays(count = 7, today = today).let { Week(it.first(), it.drop(1)) }

    /** The week up to each date in [dates], read again whenever a day is saved. */
    fun weeks(dates: Flow<LocalDate>): Flow<Week> = combine(dates, changes) { date, _ -> week(date) }
}

/** [today] and the days before it, newest first. */
data class Week(val today: DayEntry, val history: List<DayEntry>)
