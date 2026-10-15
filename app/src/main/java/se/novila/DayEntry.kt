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
    val food: FoodLog = FoodLog(),
) {
    /** Rounds to the nearest half hour within 0–12 h. */
    fun withSleep(hours: Float) =
        copy(sleepHours = (Math.round(hours * 2) / 2f).coerceIn(0f, MAX_SLEEP_HOURS))

    fun withMood(value: Int) = copy(mood = value.coerceIn(0, 5))
}

/**
 * Simple versioned text format without Android dependencies, so it can be unit tested and migrated.
 * v2: "v2;water;sleep;mood;vegetables;meals;protein;wholeGrain;treats".
 * v1 ("v1;water;sleep;mood", before food logging) is still read, with nothing eaten logged.
 */
object DayEntryCodec {
    private const val VERSION = "v2"

    fun encode(entry: DayEntry): String {
        val food = with(entry.food) { listOf(vegetables, meals, protein, wholeGrain, treats) }
        return (listOf(VERSION, entry.waterGlasses, entry.sleepHours, entry.mood) + food).joinToString(";")
    }

    fun decode(date: LocalDate, raw: String?): DayEntry {
        val parts = raw?.split(';') ?: return DayEntry(date)
        val valid = (parts.size == 4 && parts[0] == "v1") || (parts.size == 9 && parts[0] == VERSION)
        if (!valid) return DayEntry(date)
        fun int(index: Int) = parts.getOrNull(index)?.toIntOrNull() ?: 0
        return DayEntry(
            date = date,
            waterGlasses = int(1),
            sleepHours = parts[2].toFloatOrNull() ?: 0f,
            mood = int(3),
            food = FoodLog(vegetables = int(4), meals = int(5), protein = int(6), wholeGrain = int(7), treats = int(8)),
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
