package se.novila.model

import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

const val WATER_GOAL = 8
const val MAX_SLEEP_HOURS = 12f

/**
 * Health data for one day. mood: 0 = not set, 1–5 = bad → great. [strengthMinutes] is the time spent
 * in strength sessions done in Novila's session mode; it is the history the training program builds on.
 */
data class DayEntry(
    val date: LocalDate,
    val waterGlasses: Int = 0,
    val sleepHours: Float = 0f,
    val mood: Int = 0,
    val food: FoodLog = FoodLog(),
    val strengthMinutes: Int = 0,
) {
    /** Rounds to the nearest half hour within 0–12 h. */
    fun withSleep(hours: Float) =
        copy(sleepHours = (Math.round(hours * 2) / 2f).coerceIn(0f, MAX_SLEEP_HOURS))

    fun withMood(value: Int) = copy(mood = value.coerceIn(0, 5))

    /** Adds a finished strength session. A day holds at most [MAX_STRENGTH_MINUTES]. */
    fun withStrengthSession(minutes: Int) =
        copy(strengthMinutes = (strengthMinutes + minutes.coerceAtLeast(1)).coerceAtMost(MAX_STRENGTH_MINUTES))
}

const val MAX_STRENGTH_MINUTES = 600

/**
 * Simple versioned text format without Android dependencies, so it can be unit tested and migrated.
 * v3: "v3;water;sleep;mood;vegetables;meals;protein;wholeGrain;treats;strengthMinutes".
 * Older versions are still read: v2 (before session mode) with no strength sessions, and v1 (before
 * food logging) with nothing eaten logged either.
 */
object DayEntryCodec {
    private const val VERSION = "v3"
    private val FIELDS = mapOf("v1" to 4, "v2" to 9, VERSION to 10)

    fun encode(entry: DayEntry): String {
        val food = with(entry.food) { listOf(vegetables, meals, protein, wholeGrain, treats) }
        return (listOf(VERSION, entry.waterGlasses, entry.sleepHours, entry.mood) + food + entry.strengthMinutes)
            .joinToString(";")
    }

    fun decode(date: LocalDate, raw: String?): DayEntry {
        val parts = raw?.split(';') ?: return DayEntry(date)
        if (FIELDS[parts[0]] != parts.size) return DayEntry(date)
        fun int(index: Int) = parts.getOrNull(index)?.toIntOrNull() ?: 0
        return DayEntry(
            date = date,
            waterGlasses = int(1),
            sleepHours = parts[2].toFloatOrNull() ?: 0f,
            mood = int(3),
            food = FoodLog(vegetables = int(4), meals = int(5), protein = int(6), wholeGrain = int(7), treats = int(8)),
            strengthMinutes = int(9),
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
