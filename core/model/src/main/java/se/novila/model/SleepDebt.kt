package se.novila.model

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * How far the last [DEBT_NIGHTS] nights fall short of the sleep goal, in [minutes]: each logged night's shortfall,
 * with the last week counting in full and the week before it half, so recent nights weigh most. A night longer than
 * the goal pays some back, but the debt never goes below zero. [nights] were logged in the window; [minutes] is null
 * with fewer than [MIN_DEBT_NIGHTS], since a few nights say little. Unlogged nights are unknown, never short.
 */
data class SleepDebt(val minutes: Int?, val nights: Int) {
    /** A debt worth a word: about half an hour short a night over the last week, or more. */
    val notable: Boolean get() = (minutes ?: 0) >= NOTABLE_DEBT_MINUTES
}

/**
 * The sleep debt up to [today] from the [days] (any order) against the sleep goal in [goals]. Deterministic, so it is
 * unit tested.
 */
fun sleepDebt(days: List<DayEntry>, goals: Goals, today: LocalDate): SleepDebt {
    val first = today.minusDays(DEBT_NIGHTS - 1)
    val logged = days.filter { it.date in first..today && it.sleepHours > 0f }
    if (logged.size < MIN_DEBT_NIGHTS) return SleepDebt(minutes = null, nights = logged.size)
    val recentStart = today.minusDays(DAYS_PER_WEEK - 1)
    val debt = logged.sumOf { day ->
        val short = (goals.sleepHours - day.sleepHours) * MINUTES_PER_HOUR.toDouble()
        if (day.date >= recentStart) short else short * EARLIER_WEEK_WEIGHT
    }
    return SleepDebt(minutes = debt.coerceAtLeast(0.0).roundToInt(), nights = logged.size)
}

/** Two weeks: long enough to see a debt build up, short enough that old nights are paid off. */
const val DEBT_NIGHTS = 14L

/** Nights logged in the two weeks before the debt says anything. */
const val MIN_DEBT_NIGHTS = 5

/** Four hours, about 35 minutes short a night over a week. */
const val NOTABLE_DEBT_MINUTES = 240

private const val DAYS_PER_WEEK = 7L
private const val EARLIER_WEEK_WEIGHT = 0.5
private const val MINUTES_PER_HOUR = 60
