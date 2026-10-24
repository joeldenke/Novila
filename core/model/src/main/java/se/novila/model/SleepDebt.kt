package se.novila.model

import se.novila.science.decayWeight
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * How far the last [DEBT_NIGHTS] nights fall short of the sleep goal, in [minutes]: each logged night's shortfall,
 * fading with a half-life of [DEBT_HALF_LIFE_DAYS] days, so last night counts in full, a night a week ago half and
 * there is no edge where a night suddenly stops counting. A night longer than the goal pays back, but only
 * [PAYBACK_SHARE] of its extra time: catching up on lost sleep takes longer than losing it (Kitamura et al. 2016),
 * and the debt never goes below zero. [nights] were logged in the window; [minutes] is null with fewer than
 * [MIN_DEBT_NIGHTS], since a few nights say little. Unlogged nights are unknown, never short.
 */
data class SleepDebt(val minutes: Int?, val nights: Int) {
    /** A debt worth a word: about half an hour short a night over the two weeks, or more. */
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
    val debt = logged.sumOf { day ->
        val short = (goals.sleepHours - day.sleepHours) * MINUTES_PER_HOUR.toDouble()
        val age = ChronoUnit.DAYS.between(day.date, today).toDouble()
        decayWeight(age, DEBT_HALF_LIFE_DAYS) * if (short >= 0) short else short * PAYBACK_SHARE
    }
    return SleepDebt(minutes = debt.coerceAtLeast(0.0).roundToInt(), nights = logged.size)
}

/** Two weeks: long enough to see a debt build up, short enough that old nights are paid off. */
const val DEBT_NIGHTS = 14L

/** Nights logged in the two weeks before the debt says anything. */
const val MIN_DEBT_NIGHTS = 5

/** Four hours: about half an hour short every night of the two weeks, whose weights add up to about eight nights. */
const val NOTABLE_DEBT_MINUTES = 240

/** A night's shortfall counts half after a week. */
const val DEBT_HALF_LIFE_DAYS = 7.0

/** The share of a long night's extra time that pays back the debt. */
const val PAYBACK_SHARE = 0.5
private const val MINUTES_PER_HOUR = 60
