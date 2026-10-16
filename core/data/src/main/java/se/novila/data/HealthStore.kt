package se.novila.data

import android.content.Context
import se.novila.model.DayEntry
import se.novila.model.DayEntryCodec
import java.time.LocalDate

/** Local storage: one encoded [DayEntry] per date in SharedPreferences. */
class HealthStore(context: Context) {
    private val prefs = context.getSharedPreferences("health", Context.MODE_PRIVATE)

    fun load(date: LocalDate): DayEntry =
        DayEntryCodec.decode(date, prefs.getString(date.toString(), null))

    fun save(entry: DayEntry) {
        prefs.edit().putString(entry.date.toString(), DayEntryCodec.encode(entry)).apply()
    }

    fun lastDays(count: Int, today: LocalDate): List<DayEntry> =
        (0 until count).map { load(today.minusDays(it.toLong())) }
}
