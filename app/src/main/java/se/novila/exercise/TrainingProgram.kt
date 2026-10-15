package se.novila.exercise

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/** One planned workout in the week. */
data class PlannedSession(
    val day: DayOfWeek,
    val type: ExerciseType,
    val minutes: Int,
)

/** A week of workouts starting on Monday [weekStart], sorted by day. */
data class TrainingProgram(
    val weekStart: LocalDate,
    val cardioMinutes: Int,
    val sessions: List<PlannedSession>,
)

/**
 * Builds a week's program from recent workouts, along WHO's guidelines for adults: 150–300 minutes of
 * moderate cardio and strength training on at least two days a week. It starts where the user is and
 * raises the cardio time by at most 10 % a week, so a beginner gets a gentle program and an active user
 * isn't held back. Plain Kotlin, so it is unit tested; the source of the workouts doesn't matter.
 */
object TrainingProgramGenerator {
    const val MIN_CARDIO_MINUTES = 90
    const val MAX_CARDIO_MINUTES = 300
    private const val GUIDELINE_CARDIO_MINUTES = 150
    private const val HISTORY_WEEKS = 4L
    private const val PROGRESSION = 1.1
    private const val MOBILITY_MINUTES = 15
    private val STRENGTH_DAYS = listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
    private val THREE_CARDIO_DAYS = listOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
    private val FOUR_CARDIO_DAYS = listOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun generate(history: List<Exercise>, today: LocalDate): TrainingProgram {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val recent = history.filter { it.date >= weekStart.minusWeeks(HISTORY_WEEKS) && it.date < weekStart }

        val weeklyCardio = recent.filter { it.type.kind == ExerciseKind.Cardio }
            .sumOf { it.duration.toMinutes() } / HISTORY_WEEKS.toDouble()
        val cardioTarget = roundTo5(weeklyCardio * PROGRESSION).coerceIn(MIN_CARDIO_MINUTES, MAX_CARDIO_MINUTES)
        val cardioDays = if (cardioTarget < GUIDELINE_CARDIO_MINUTES) THREE_CARDIO_DAYS else FOUR_CARDIO_DAYS
        val cardioType = favoriteCardio(recent)

        val weeklyStrength = recent.count { it.type.kind == ExerciseKind.Strength } / HISTORY_WEEKS.toDouble()
        val strengthMinutes = when {
            weeklyStrength >= 2 -> 40
            weeklyStrength >= 1 -> 30
            else -> 20
        }

        val cardioMinutes = roundTo5(cardioTarget / cardioDays.size.toDouble())
        val sessions = cardioDays.map { PlannedSession(it, cardioType, cardioMinutes) } +
            STRENGTH_DAYS.map { PlannedSession(it, ExerciseType.Strength, strengthMinutes) } +
            PlannedSession(DayOfWeek.WEDNESDAY, ExerciseType.Mobility, MOBILITY_MINUTES)

        return TrainingProgram(
            weekStart = weekStart,
            cardioMinutes = cardioTarget,
            sessions = sessions.sortedBy { it.day },
        )
    }

    /** The cardio the user does most, so the program fits their habits. Walking for a new user. */
    private fun favoriteCardio(recent: List<Exercise>): ExerciseType = recent
        .filter { it.type.kind == ExerciseKind.Cardio && it.type != ExerciseType.Other }
        .groupingBy { it.type }
        .eachCount()
        .maxWithOrNull(compareBy<Map.Entry<ExerciseType, Int>> { it.value }.thenBy { -it.key.ordinal })
        ?.key ?: ExerciseType.Walking

    private fun roundTo5(minutes: Double): Int = (minutes / 5).roundToInt() * 5
}
