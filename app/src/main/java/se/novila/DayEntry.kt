package se.novila

import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

const val WATER_GOAL = 8
const val MAX_SLEEP_HOURS = 12f

/** Health data for one day. mood: 0 = not set, 1–5 = bad → great. */
data class DayEntry(
    val date: LocalDate,
    val waterGlasses: Int = 0,
    val sleepHours: Float = 0f,
    val mood: Int = 0,
) {
    val waterProgress: Float get() = (waterGlasses.toFloat() / WATER_GOAL).coerceIn(0f, 1f)

    fun withWater(delta: Int) = copy(waterGlasses = (waterGlasses + delta).coerceAtLeast(0))

    /** Rounds to the nearest half hour within 0–12 h. */
    fun withSleep(hours: Float) =
        copy(sleepHours = (Math.round(hours * 2) / 2f).coerceIn(0f, MAX_SLEEP_HOURS))

    fun withMood(value: Int) = copy(mood = value.coerceIn(0, 5))
}

/**
 * Simple versioned text format ("v1;water;sleep;mood") without Android dependencies,
 * so it can be unit tested and migrated if the format changes.
 */
object DayEntryCodec {
    private const val VERSION = "v1"

    fun encode(entry: DayEntry): String =
        listOf(VERSION, entry.waterGlasses, entry.sleepHours, entry.mood).joinToString(";")

    fun decode(date: LocalDate, raw: String?): DayEntry {
        val parts = raw?.split(';') ?: return DayEntry(date)
        if (parts.size != 4 || parts[0] != VERSION) return DayEntry(date)
        return DayEntry(
            date = date,
            waterGlasses = parts[1].toIntOrNull() ?: 0,
            sleepHours = parts[2].toFloatOrNull() ?: 0f,
            mood = parts[3].toIntOrNull() ?: 0,
        )
    }
}

/**
 * Hours as a localized number ("7", "7,5" in Swedish, "7.5" in English),
 * or null when nothing is logged. The unit is added by the UI from string resources.
 */
fun formatHours(hours: Float, locale: Locale): String? {
    if (hours <= 0f) return null
    val format = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 1
    }
    return format.format(hours)
}
