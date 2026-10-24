package se.novila.model

import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlin.math.roundToInt

const val WATER_GOAL = 8
const val MAX_SLEEP_HOURS = 12f

/** Cardio the user can log by hand ("I did it"). The names match the training program's exercise types. */
enum class CardioType { Walking, Running, Cycling, Swimming }

/**
 * A cardio session logged by hand, with how hard it felt when the user said so ([effort], RPE 1–10 on the Borg CR10
 * scale, null when not given), so the training load counts it as it was instead of as a moderate session.
 */
data class CardioLog(val type: CardioType, val minutes: Int, val effort: Int? = null)

/**
 * What may have affected a night, logged with it the morning after. Most are about the evening before: caffeine late
 * in the day, screens in bed, alcohol, stress, a late meal or hard exercise late. [Period] is about the night itself:
 * cramps and poorer sleep around a period are common, and it is not a habit, so the sleep coach never gives a tip
 * about it. It asks nothing about gender: anyone who has a period can mark it.
 */
enum class SleepFactor { LateCaffeine, Screens, Alcohol, Stress, LateMeal, LateExercise, Period }

/** How rested the user felt, 1 (poor) to 5 (great). */
const val MAX_SLEEP_QUALITY = 5

/**
 * Health data for one day. [drinks] is everything drunk, in the order it was logged ([Drink]). mood: 0 = not set, 1–5 =
 * bad → great, energy: 0 = not set, 1–5 = exhausted → full of energy, and stress: 0 = not set, 1–5 = calm → very
 * stressed. [diary] is a free line about the day, at most [MAX_DIARY_LENGTH] characters. [strengthMinutes] is the time
 * spent in strength sessions done in Novila's session mode and [cardio] the cardio logged by hand; together they are
 * the history the training program builds on.
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
    val drinks: List<Drink> = emptyList(),
    val sleepHours: Float = 0f,
    val mood: Int = 0,
    val energy: Int = 0,
    val stress: Int = 0,
    val diary: String = "",
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

    /** Glasses ([Goals.GLASS_ML]) towards the water goal, whole ones: what [waterMl] adds up to. */
    val waterGlasses: Int get() = waterMl / Goals.GLASS_ML

    /** Rounds to the nearest half hour within 0–12 h. The times no longer match the hours, so they are cleared. */
    fun withSleep(hours: Float) = copy(
        sleepHours = hours.roundToHalfHour().coerceIn(0f, MAX_SLEEP_HOURS),
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

    fun withEnergy(value: Int) = copy(energy = value.coerceIn(0, 5))

    fun withStress(value: Int) = copy(stress = value.coerceIn(0, 5))

    /** One short diary line: trimmed, one line, at most [MAX_DIARY_LENGTH] characters. */
    fun withDiary(text: String) = copy(diary = diaryLine(text))

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
        return copy(
            cardio = cardio + log.copy(
                minutes = log.minutes.coerceIn(1, room),
                effort = log.effort?.coerceIn(1, MAX_EFFORT),
            ),
        )
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

/** How long a diary line may be: one short line, at most [MAX_DIARY_LENGTH] characters. */
const val MAX_DIARY_LENGTH = 140

/**
 * The diary line as it is stored: one line, so newlines become spaces, and without semicolons (the codec's field
 * separator), which become commas.
 */
private fun diaryLine(text: String): String =
    text.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').replace(';', ',').trim().take(MAX_DIARY_LENGTH)

/**
 * Simple versioned text format without Android dependencies, so it can be unit tested and migrated. v9:
 * "v9;water;sleep;mood;vegetables;meals;protein;wholeGrain;treats;strengthMinutes;cardio;effort;pain;rehabPhase;
 * bedtime;wakeTime;sleepQuality;sleepFactors;energy;stress;diary;drinks;redMeat;fish", where cardio is comma-separated
 * "type:minutes:effort" (empty when none, the effort empty when not given; before v9 "type:minutes"), pain is empty
 * when not given, the times are minutes after midnight (empty when not given) and the factors are comma-separated
 * names (a new factor needs no new version: an older app skips names it doesn't know). Every version only appends
 * fields, so older versions decode with the same indices and the newer fields default; v6 appends energy (0 = not set,
 * 1–5), stress (0 = not set, 1–5) and the diary line, which encode keeps free of semicolons so it never breaks the
 * format. v7 appends drinks, comma-separated "kind:ml:time" with the time in minutes after midnight (empty when not
 * known). From v7 the water field is empty: water is a drink. Before v7 it was glasses of water, read as that many
 * glasses without a time. v8 appends portions of red meat and charcuterie, which before were logged as protein. v9
 * appends portions of fish and seafood, which before were logged as protein too, and gives each cardio session its
 * effort. Older versions are still read: v8 (before fish and cardio efforts, read as none), v7 (before red meat, read
 * as none), v6 (before drinks), v5 (before energy, stress and the diary line), v4 (before bedtimes, sleep quality and
 * factors), v3 (before cardio logging and feedback after a session), v2 (before session mode) with no strength
 * sessions either, and v1 (before food logging) with nothing eaten logged either.
 */
object DayEntryCodec {
    private const val VERSION = "v9"
    private val FIELDS = mapOf(
        "v1" to 4,
        "v2" to 9,
        "v3" to 10,
        "v4" to 14,
        "v5" to 18,
        "v6" to 21,
        "v7" to 22,
        "v8" to 23,
        VERSION to 24,
    )

    fun encode(entry: DayEntry): String {
        val day = listOf(VERSION, "", entry.sleepHours, entry.mood)
        val food = with(entry.food) { listOf(vegetables, meals, protein, wholeGrain, treats) }
        val cardio = entry.cardio.joinToString(",") { "${it.type.name}:${it.minutes}:${it.effort ?: ""}" }
        val training = listOf(entry.strengthMinutes, cardio, entry.effort, entry.pain ?: "", entry.rehabPhase)
        val night = listOf(
            entry.bedtime?.let(::minutes) ?: "",
            entry.wakeTime?.let(::minutes) ?: "",
            entry.sleepQuality,
            entry.sleepFactors.sorted().joinToString(",") { it.name },
        )
        val moment = listOf(entry.energy, entry.stress, diaryLine(entry.diary))
        val drinks = entry.drinks.joinToString(",") { "${it.kind.name}:${it.ml}:${it.time?.let(::minutes) ?: ""}" }
        val laterFood = listOf(entry.food.redMeat, entry.food.fish)
        return (day + food + training + night + moment + drinks + laterFood).joinToString(";")
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
            drinks = if (parts[0] in DRINK_VERSIONS) drinks(parts[21]) else glassesOfWater(int(1)),
            sleepHours = parts[2].toFloatOrNull() ?: 0f,
            mood = int(3),
            food = FoodLog(
                vegetables = int(4),
                meals = int(5),
                protein = int(6),
                wholeGrain = int(7),
                treats = int(8),
                redMeat = int(22),
                fish = int(23),
            ),
            strengthMinutes = int(9),
            cardio = cardio(parts.getOrNull(10)),
            effort = int(11).coerceIn(0, MAX_EFFORT),
            pain = parts.getOrNull(12)?.toIntOrNull()?.coerceIn(0, MAX_PAIN),
            rehabPhase = int(13).coerceAtLeast(0),
            bedtime = time(parts.getOrNull(14)),
            wakeTime = time(parts.getOrNull(15)),
            sleepQuality = int(16).coerceIn(0, MAX_SLEEP_QUALITY),
            sleepFactors = factors(parts.getOrNull(17)),
            energy = int(18).coerceIn(0, 5),
            stress = int(19).coerceIn(0, 5),
            diary = diaryLine(parts.getOrNull(20).orEmpty()),
        )
    }

    /** Unknown kinds (from a later version) and unreadable amounts are skipped. */
    private fun drinks(raw: String): List<Drink> = raw.split(',').mapNotNull { part ->
        val fields = part.split(':')
        val kind = DrinkKind.entries.firstOrNull { it.name == fields[0] }
        val ml = fields.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }?.coerceAtMost(MAX_DRINK_ML)
        if (kind != null && ml != null) Drink(kind, ml, time(fields.getOrNull(2))) else null
    }.take(MAX_DRINKS)

    /** Unknown factors (from a later version) are skipped. */
    private fun factors(raw: String?): Set<SleepFactor> =
        raw.orEmpty().split(',').mapNotNull { name -> SleepFactor.entries.firstOrNull { it.name == name } }.toSet()

    /** The versions whose drinks are in their own field, after water became a drink. */
    private val DRINK_VERSIONS = setOf("v7", "v8", VERSION)

    /** Unknown types (from a later version), unreadable minutes and efforts out of range are skipped. */
    private fun cardio(raw: String?): List<CardioLog> = raw.orEmpty().split(',').mapNotNull { part ->
        val fields = part.split(':')
        val type = CardioType.entries.firstOrNull { it.name == fields[0] }
        val minutes = fields.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }
        val effort = fields.getOrNull(2)?.toIntOrNull()?.takeIf { it in 1..MAX_EFFORT }
        if (type != null && minutes != null) CardioLog(type, minutes, effort) else null
    }
}

/**
 * Sleep hours rounded to the nearest half hour, the step sleep is logged and shown in.
 */
fun Float.roundToHalfHour(): Float = (this * 2).roundToInt() / 2f

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
