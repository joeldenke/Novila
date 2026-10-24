package se.novila.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt

/** How regular the sleep is, from the [SleepRegularity.index]. */
enum class RegularityLevel { Steady, FairlySteady, Irregular }

/**
 * How regular the user's sleep is over the last [REGULARITY_NIGHTS] nights, from the bedtimes and wake-ups they
 * logged (or a watch recorded).
 *
 * [index] is the Sleep Regularity Index (Phillips et al. 2017): the chance of being in the same state, asleep or
 * awake, at the same time of day on two days in a row, scaled from -100 to 100 (100 is the same sleep every day). It
 * is computed over [pairs] pairs of nights in a row that both have times, and is null with fewer than
 * [MIN_REGULARITY_PAIRS]. Regularity is linked to health in studies at least as strongly as how long the nights are.
 *
 * [socialJetlagMinutes] is how much later the middle of the sleep is on weekend mornings than on weekday mornings
 * (negative when earlier), from [weekendNights] and [weekdayNights], or null with too few of either.
 */
data class SleepRegularity(
    val index: Int?,
    val pairs: Int,
    val socialJetlagMinutes: Int?,
    val weekdayNights: Int,
    val weekendNights: Int,
) {
    val level: RegularityLevel?
        get() = index?.let {
            when {
                it >= STEADY_INDEX -> RegularityLevel.Steady
                it >= FAIRLY_STEADY_INDEX -> RegularityLevel.FairlySteady
                else -> RegularityLevel.Irregular
            }
        }

    /** The weekend shifts the sleep by [NOTABLE_JETLAG_MINUTES] or more, earlier or later. */
    val notableJetlag: Boolean get() = abs(socialJetlagMinutes ?: 0) >= NOTABLE_JETLAG_MINUTES

    val isEmpty: Boolean get() = index == null && socialJetlagMinutes == null
}

/**
 * The regularity of the nights in [days] up to [today]. Each day's night runs from its bedtime the evening before to
 * its wake-up, so only days with both times count; nights without them are unknown, never "irregular".
 * Deterministic, so it is unit tested.
 */
fun sleepRegularity(days: List<DayEntry>, today: LocalDate): SleepRegularity {
    val first = today.minusDays(REGULARITY_NIGHTS - 1)
    val nights = days.filter { it.date in first..today }.mapNotNull { day -> day.sleepSpan()?.let { day.date to it } }
        .toMap()
    val agreements = nights.mapNotNull { (date, span) ->
        nights[date.plusDays(1)]?.let { next -> span.agreement(next) }
    }
    val (weekend, weekday) = nights.entries.partition { it.key.dayOfWeek in WEEKEND_MORNINGS }
    val jetlag = if (weekend.size >= MIN_WEEKEND_NIGHTS && weekday.size >= MIN_WEEKDAY_NIGHTS) {
        (weekend.map { it.value.middle }.average() - weekday.map { it.value.middle }.average()).roundToInt()
    } else {
        null
    }
    return SleepRegularity(
        index = agreements.takeIf { it.size >= MIN_REGULARITY_PAIRS }?.let { (200 * it.average() - 100).roundToInt() },
        pairs = agreements.size,
        socialJetlagMinutes = jetlag,
        weekdayNights = weekday.size,
        weekendNights = weekend.size,
    )
}

/**
 * The user's body clock from the nights before free days ([freeNights], Saturday and Sunday mornings) and before work
 * days ([workNights]), over the last [REGULARITY_NIGHTS] nights: [midSleep] is the middle of the sleep on free days,
 * when no alarm decides it, corrected for the sleep the work week builds up (MSFsc, Roenneberg et al. 2004). When free
 * nights are longer than the week's average, half the difference is catch-up sleep, not the body clock.
 */
data class BodyClock(val midSleep: LocalTime, val freeNights: Int, val workNights: Int) {
    /** The bedtime that puts [hours] of sleep around [midSleep], rounded to a quarter of an hour. */
    fun bedtimeFor(hours: Float): LocalTime {
        val minutes = (midSleep.hour * 60 + midSleep.minute - hours * 60 / 2).roundToInt()
        val rounded = ((minutes.toDouble() / QUARTER).roundToInt() * QUARTER + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return LocalTime.of(rounded / 60, rounded % 60)
    }
}

/**
 * The body clock from the nights in [days] up to [today] with both a bedtime and a wake-up, or null with too few free
 * or work nights. Deterministic, so it is unit tested.
 */
fun bodyClock(days: List<DayEntry>, today: LocalDate): BodyClock? {
    val first = today.minusDays(REGULARITY_NIGHTS - 1)
    val nights = days.filter { it.date in first..today }.mapNotNull { day -> day.sleepSpan()?.let { day.date to it } }
    val (free, work) = nights.partition { it.first.dayOfWeek in WEEKEND_MORNINGS }
    if (free.size < MIN_WEEKEND_NIGHTS || work.size < MIN_WEEKDAY_NIGHTS) return null
    val freeLength = free.map { it.second.length }.average()
    val workLength = work.map { it.second.length }.average()
    val weekLength = (WORK_DAYS * workLength + FREE_DAYS * freeLength) / (WORK_DAYS + FREE_DAYS)
    val catchUp = (freeLength - weekLength).coerceAtLeast(0.0) / 2
    val middle = (free.map { it.second.middle }.average() - catchUp + NOON).roundToInt() % MINUTES_PER_DAY
    return BodyClock(LocalTime.of(middle / 60, middle % 60), freeNights = free.size, workNights = work.size)
}

/**
 * A night as minutes into the day it belongs to, which runs from noon before to noon on the morning: asleep from
 * [start] to [end]. Noon to noon, so 23:30 and 00:30 are an hour apart and the night is one piece.
 */
private class SleepSpan(val start: Int, val end: Int) {
    val middle: Double get() = (start + end) / 2.0

    val length: Int get() = end - start

    /** The share of the day both nights spend in the same state, asleep or awake. */
    fun agreement(other: SleepSpan): Double {
        val overlap = (minOf(end, other.end) - maxOf(start, other.start)).coerceAtLeast(0)
        val different = (end - start) + (other.end - other.start) - 2 * overlap
        return 1.0 - different.toDouble() / MINUTES_PER_DAY
    }
}

private fun DayEntry.sleepSpan(): SleepSpan? {
    val bed = bedtime ?: return null
    val wake = wakeTime ?: return null
    val start = fromNoon(bed)
    val minutes = Duration.between(bed, wake).toMinutes().let { if (it < 0) it + MINUTES_PER_DAY else it }.toInt()
    if (minutes == 0) return null
    return SleepSpan(start, (start + minutes).coerceAtMost(MINUTES_PER_DAY))
}

private fun fromNoon(time: LocalTime): Int = (time.hour * 60 + time.minute - NOON + MINUTES_PER_DAY) % MINUTES_PER_DAY

/** The nights the regularity looks at: four weeks, so the weekends count four times. */
const val REGULARITY_NIGHTS = 28L

/** Pairs of nights in a row before the index says anything. */
const val MIN_REGULARITY_PAIRS = 5

/**
 * An index of 80 or more is steady, about the middle of the UK Biobank's 61,000 adults or better (median 81), and under
 * 72 irregular, their least regular fifth (under 71.6), where the risk of dying early was clearly higher (Windred et
 * al. 2024). Novila's index comes from logged bedtimes and wake-ups rather than a wrist sensor, so it misses naps and
 * time awake in the night and reads somewhat higher; the bands are meant as a rough guide, not a diagnosis.
 */
const val STEADY_INDEX = 80
const val FAIRLY_STEADY_INDEX = 72

/** A weekend shift of an hour or more is worth a word. */
const val NOTABLE_JETLAG_MINUTES = 60

private const val MIN_WEEKEND_NIGHTS = 2
private const val MIN_WEEKDAY_NIGHTS = 3

/** The nights before a free day: Saturday and Sunday mornings. */
private val WEEKEND_MORNINGS = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** Five work days and two free days in a week, for the week's average night. */
private const val WORK_DAYS = 5
private const val FREE_DAYS = 2
private const val QUARTER = 15

private const val NOON = 12 * 60
private const val MINUTES_PER_DAY = 24 * 60
