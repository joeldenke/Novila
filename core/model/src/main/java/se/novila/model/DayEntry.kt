package se.novila.model

import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

const val WATER_GOAL = 8
const val MAX_SLEEP_HOURS = 12f

/** Cardio the user can log by hand ("I did it"). The names match the training program's exercise types. */
enum class CardioType { Walking, Running, Cycling, Swimming }

/** A cardio session logged by hand. */
data class CardioLog(val type: CardioType, val minutes: Int)

/**
 * What may have affected a night, logged with it the morning after. Each is about the evening before: caffeine late
 * in the day, screens in bed, alcohol, stress, a late meal or hard exercise late.
 */
enum class SleepFactor { LateCaffeine, Screens, Alcohol, Stress, LateMeal, LateExercise }

/** How rested the user felt, 1 (poor) to 5 (great). */
const val MAX_SLEEP_QUALITY = 5

/**
 * Health data for one day. mood: 0 = not set, 1–5 = bad → great. [strengthMinutes] is the time spent
 * in strength sessions done in Novila's session mode and [cardio] the cardio logged by hand; together they are the
 * history the training program builds on.
 *
 * After a strength session the user can say how hard it was ([effort], RPE 1–10, 0 = not given) and how much it hurt
 * ([pain], 0–10, null = not given). [rehabPhase] is the rehab phase the session was done in, 0 for other sessions,
 * so the coach knows which phase the pain belongs to.
 *
 * The night is the one before [date], logged in the morning: [sleepHours], and, when the user gives them, [bedtime]
 * the evening before and [wakeTime] this morning (the hours then follow from them), how rested they felt
 * ([sleepQuality], 1–5, 0 = not given) and what may have affected the night ([sleepFactors]).
 */
data class DayEntry(
    val date: LocalDate,
    val waterGlasses: Int = 0,
    val sleepHours: Float = 0f,
    val mood: Int = 0,
    val food: FoodLog = FoodLog(),
    val strengthMinutes: Int = 0,
    val cardio: List<CardioLog> = emptyList(),
    val effort: Int = 0,
    val pain: Int? = null,
    val rehabPhase: Int = 0,
    val bedtime: LocalTime? = null,
    val wakeTime: LocalTime? = null,
    val sleepQuality: Int = 0,
    val sleepFactors: Set<SleepFactor> = emptySet(),
) {
    val cardioMinutes: Int get() = cardio.sumOf { it.minutes }

    /** Rounds to the nearest half hour within 0–12 h. The times no longer match the hours, so they are cleared. */
    fun withSleep(hours: Float) = copy(
        sleepHours = (Math.round(hours * 2) / 2f).coerceIn(0f, MAX_SLEEP_HOURS),
        bedtime = null,
        wakeTime = null,
    )

    /**
     * When the user went to bed and woke up; the hours follow, to the minute, across midnight and at most
     * [MAX_SLEEP_HOURS]. The same time for both is no sleep.
     */
    fun withSleepTimes(bedtime: LocalTime, wakeTime: LocalTime): DayEntry {
        val minutes = Duration.between(bedtime, wakeTime).toMinutes().let { if (it < 0) it + MINUTES_PER_DAY else it }
        return copy(
            sleepHours = (minutes / 60f).coerceAtMost(MAX_SLEEP_HOURS),
            bedtime = bedtime,
            wakeTime = wakeTime,
        )
    }

    fun withSleepQuality(quality: Int) = copy(sleepQuality = quality.coerceIn(0, MAX_SLEEP_QUALITY))

    /** Turns [factor] on or off for the night. */
    fun withSleepFactor(factor: SleepFactor, on: Boolean) =
        copy(sleepFactors = if (on) sleepFactors + factor else sleepFactors - factor)

    fun withMood(value: Int) = copy(mood = value.coerceIn(0, 5))

    /** Adds a finished strength session. A day holds at most [MAX_STRENGTH_MINUTES]. */
    fun withStrengthSession(minutes: Int) =
        copy(strengthMinutes = (strengthMinutes + minutes.coerceAtLeast(1)).coerceAtMost(MAX_STRENGTH_MINUTES))

    /**
     * Adds what the user said after a strength session. A second session the same day keeps the harder effort and the
     * worse pain, so the coach never misses a hard day. [phase] is the session's rehab phase, 0 when it wasn't rehab.
     */
    fun withFeedback(effort: Int?, pain: Int?, phase: Int = 0) = copy(
        effort = maxOf(this.effort, effort?.coerceIn(1, MAX_EFFORT) ?: 0),
        pain = listOfNotNull(this.pain, pain?.coerceIn(0, MAX_PAIN)).maxOrNull(),
        rehabPhase = if (phase > 0) phase else rehabPhase,
    )

    /** Adds a cardio session logged by hand. A day holds at most [MAX_CARDIO_MINUTES]. */
    fun withCardio(log: CardioLog): DayEntry {
        val room = MAX_CARDIO_MINUTES - cardioMinutes
        if (room <= 0) return this
        return copy(cardio = cardio + log.copy(minutes = log.minutes.coerceIn(1, room)))
    }

    /** Takes back the last cardio session logged by hand. */
    fun withoutLastCardio() = copy(cardio = cardio.dropLast(1))
}

private const val MINUTES_PER_DAY = 24 * 60

const val MAX_STRENGTH_MINUTES = 600
const val MAX_CARDIO_MINUTES = 600

/** Effort on the Borg CR10 scale (RPE): 1 is very easy, 10 is all you have. */
const val MAX_EFFORT = 10
const val MAX_PAIN = 10

/**
 * Simple versioned text format without Android dependencies, so it can be unit tested and migrated.
 * v5: "v5;water;sleep;mood;vegetables;meals;protein;wholeGrain;treats;strengthMinutes;cardio;effort;pain;rehabPhase;
 * bedtime;wakeTime;sleepQuality;sleepFactors", where cardio is comma-separated "type:minutes" (empty when none), pain
 * is empty when not given, the times are minutes after midnight (empty when not given) and the factors are
 * comma-separated names.
 * Older versions are still read: v4 (before bedtimes, sleep quality and factors), v3 (before cardio logging and
 * feedback after a session), v2 (before session mode) with no strength sessions either, and v1 (before food logging)
 * with nothing eaten logged either.
 */
object DayEntryCodec {
    private const val VERSION = "v5"
    private val FIELDS = mapOf("v1" to 4, "v2" to 9, "v3" to 10, "v4" to 14, VERSION to 18)

    fun encode(entry: DayEntry): String {
        val day = listOf(VERSION, entry.waterGlasses, entry.sleepHours, entry.mood)
        val food = with(entry.food) { listOf(vegetables, meals, protein, wholeGrain, treats) }
        val cardio = entry.cardio.joinToString(",") { "${it.type.name}:${it.minutes}" }
        val training = listOf(entry.strengthMinutes, cardio, entry.effort, entry.pain ?: "", entry.rehabPhase)
        val night = listOf(
            entry.bedtime?.let(::minutes) ?: "",
            entry.wakeTime?.let(::minutes) ?: "",
            entry.sleepQuality,
            entry.sleepFactors.sorted().joinToString(",") { it.name },
        )
        return (day + food + training + night).joinToString(";")
    }

    private fun minutes(time: LocalTime) = time.hour * 60 + time.minute

    private fun time(raw: String?): LocalTime? =
        raw?.toIntOrNull()?.takeIf { it in 0 until MINUTES_PER_DAY }?.let { LocalTime.of(it / 60, it % 60) }

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
            cardio = cardio(parts.getOrNull(10)),
            effort = int(11).coerceIn(0, MAX_EFFORT),
            pain = parts.getOrNull(12)?.toIntOrNull()?.coerceIn(0, MAX_PAIN),
            rehabPhase = int(13).coerceAtLeast(0),
            bedtime = time(parts.getOrNull(14)),
            wakeTime = time(parts.getOrNull(15)),
            sleepQuality = int(16).coerceIn(0, MAX_SLEEP_QUALITY),
            sleepFactors = factors(parts.getOrNull(17)),
        )
    }

    /** Unknown factors (from a later version) are skipped. */
    private fun factors(raw: String?): Set<SleepFactor> =
        raw.orEmpty().split(',').mapNotNull { name -> SleepFactor.entries.firstOrNull { it.name == name } }.toSet()

    /** Unknown types (from a later version) and unreadable minutes are skipped. */
    private fun cardio(raw: String?): List<CardioLog> = raw.orEmpty().split(',').mapNotNull { part ->
        val type = CardioType.entries.firstOrNull { it.name == part.substringBefore(':') }
        val minutes = part.substringAfter(':', "").toIntOrNull()?.takeIf { it > 0 }
        if (type != null && minutes != null) CardioLog(type, minutes) else null
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
