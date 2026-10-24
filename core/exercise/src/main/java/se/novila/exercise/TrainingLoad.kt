package se.novila.exercise

import se.novila.science.ewma
import se.novila.science.ewmaFactor
import se.novila.science.populationStandardDeviation
import java.time.LocalDate
import kotlin.math.roundToInt

/** This week against the user's usual: lighter, about the same, heavier, or not known yet. */
enum class LoadComparison { Lighter, Usual, Heavier, Unknown }

/**
 * The training load of the last seven days (session RPE, Foster): each session's effort (RPE 1–10) times its minutes,
 * summed per day. [week] is the seven days' load, [usualWeek] the usual week of the [USUAL_WEEKS] weeks before (null
 * until the history reaches that far): seven times the exponentially weighted average day (Williams et al. 2017), so
 * recent weeks weigh most and an old week fades out instead of dropping off all at once. The usual weeks never
 * include the current one, so a hard week can't raise its own yardstick, and [monotony] the days' mean load over their standard deviation (null with
 * fewer than [MIN_SESSIONS_FOR_MONOTONY] sessions). [strain] is the week times its monotony. [sessions] were trained
 * in the week, [rated] of them with the user's own effort; the others count with a moderate effort for their kind
 * ([assumedEffort]).
 *
 * The ratio of the week to the usual (acute to chronic, ACWR) is debated as a predictor of injury, so it only gives
 * calm guidance ([comparison]: "heavier than usual"), never a warning.
 */
data class TrainingLoad(
    val week: Int,
    val usualWeek: Int?,
    val monotony: Double?,
    val sessions: Int,
    val rated: Int,
) {
    val strain: Int? get() = monotony?.let { (week * it).roundToInt() }

    /** This week as a share of the usual one, e.g. 1.3 for 30 % more. */
    val ratio: Double? get() = usualWeek?.takeIf { it > 0 }?.let { week.toDouble() / it }

    val comparison: LoadComparison
        get() {
            val ratio = ratio ?: return LoadComparison.Unknown
            return when {
                ratio > HEAVIER_RATIO -> LoadComparison.Heavier
                ratio < LIGHTER_RATIO -> LoadComparison.Lighter
                else -> LoadComparison.Usual
            }
        }

    /** The days were about equally hard (Foster's monotony above [UNIFORM_MONOTONY]): an easy day would help. */
    val uniform: Boolean get() = (monotony ?: 0.0) > UNIFORM_MONOTONY
}

/**
 * The training load up to [today] from the [history] (Novila's own sessions and recorded ones, counted once) and the
 * effort the user gave after their strength sessions, by date ([efforts], RPE 1–10). Null when nothing was trained in
 * the last seven days. Deterministic, so it is unit tested.
 */
fun trainingLoad(history: List<Exercise>, efforts: Map<LocalDate, Int>, today: LocalDate): TrainingLoad? {
    val weekStart = today.minusDays(DAYS_PER_WEEK - 1)
    val week = history.filter { it.date in weekStart..today }
    if (week.isEmpty()) return null
    val load = { exercise: Exercise -> exercise.duration.inWholeMinutes * effortOf(exercise, efforts) }
    val days = (0 until DAYS_PER_WEEK).map { offset ->
        val date = weekStart.plusDays(offset)
        week.filter { it.date == date }.sumOf(load).toDouble()
    }
    val usualStart = weekStart.minusWeeks(USUAL_WEEKS)
    // The usual is only known once the history reaches back to the weeks it averages.
    val reachesBack = history.any { it.date < weekStart.minusWeeks(USUAL_WEEKS - 1) }
    val usualDays = (0 until USUAL_WEEKS * DAYS_PER_WEEK).map { offset ->
        val date = usualStart.plusDays(offset)
        history.filter { it.date == date }.sumOf(load).toDouble()
    }
    val usual = (ewma(usualDays, ewmaFactor(usualDays.size)) ?: 0.0) * DAYS_PER_WEEK
    return TrainingLoad(
        week = days.sum().roundToInt(),
        usualWeek = usual.toInt().takeIf { reachesBack },
        monotony = monotony(days).takeIf { week.size >= MIN_SESSIONS_FOR_MONOTONY },
        sessions = week.size,
        rated = week.count { it.type.kind == ExerciseKind.Strength && (efforts[it.date] ?: 0) > 0 },
    )
}

/**
 * The effort a session counts with: the user's own for a strength session, otherwise a moderate one for its kind
 * on the CR10 scale, since Novila only asks after strength sessions.
 */
fun assumedEffort(kind: ExerciseKind): Int = when (kind) {
    ExerciseKind.Strength -> 5
    ExerciseKind.Cardio -> 4
    ExerciseKind.Mobility -> 2
}

private fun effortOf(exercise: Exercise, efforts: Map<LocalDate, Int>): Int {
    val own = efforts[exercise.date]?.takeIf { it > 0 && exercise.type.kind == ExerciseKind.Strength }
    return own ?: assumedEffort(exercise.type.kind)
}

/** Mean over the (population) standard deviation; days that are all the same count as very uniform. */
private fun monotony(days: List<Double>): Double {
    val spread = populationStandardDeviation(days)
    return if (spread == 0.0) MAX_MONOTONY else (days.average() / spread).coerceAtMost(MAX_MONOTONY)
}

private const val DAYS_PER_WEEK = 7L

/** The usual week is the average of this many weeks before the current one. */
const val USUAL_WEEKS = 3L

/** More than 30 % above the usual week is heavier, more than 20 % below it lighter. */
const val HEAVIER_RATIO = 1.3
const val LIGHTER_RATIO = 0.8

/** Foster: a monotony above 2 means the days were too alike. */
const val UNIFORM_MONOTONY = 2.0

/** Monotony needs a few sessions to mean anything. */
const val MIN_SESSIONS_FOR_MONOTONY = 3

/** A cap, so a week of identical days doesn't divide by zero. */
private const val MAX_MONOTONY = 10.0
