package se.novila.exercise

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/** One planned workout in the week. Strength sessions list their [exercises]. */
data class PlannedSession(
    val day: DayOfWeek,
    val type: ExerciseType,
    val minutes: Int,
    val exercises: List<PlannedExercise> = emptyList(),
)

/** The user's recent training, which a program builds on. [strengthPerWeek] is sessions a week. */
data class RecentTraining(
    val cardioTarget: Int,
    val cardioType: ExerciseType,
    val strengthPerWeek: Double,
    val level: StrengthLevel,
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
 * isn't held back. Strength days get concrete exercises from [StrengthSessions] at the user's level.
 * Plain Kotlin, so it is unit tested; the source of the workouts doesn't matter.
 */
object TrainingProgramGenerator {
    const val MIN_CARDIO_MINUTES = 90
    const val MAX_CARDIO_MINUTES = 300
    const val GUIDELINE_CARDIO_MINUTES = 150
    private const val HISTORY_WEEKS = 4L
    private const val PROGRESSION = 1.1
    private const val MOBILITY_MINUTES = 15
    private val STRENGTH_DAYS = listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
    private val THREE_CARDIO_DAYS = listOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
    private val FOUR_CARDIO_DAYS = listOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun generate(history: List<Exercise>, today: LocalDate): TrainingProgram {
        val weekStart = weekStart(today)
        val recent = summarize(history, weekStart)
        val cardioTarget = recent.cardioTarget
        val cardioDays = if (cardioTarget < GUIDELINE_CARDIO_MINUTES) THREE_CARDIO_DAYS else FOUR_CARDIO_DAYS
        val level = recent.level
        val strengthMinutes = when (level) {
            StrengthLevel.Advanced -> 40
            StrengthLevel.Intermediate -> 30
            StrengthLevel.Beginner -> 20
        }

        val cardioMinutes = roundTo5(cardioTarget / cardioDays.size.toDouble())
        val sessions = cardioDays.map { PlannedSession(it, recent.cardioType, cardioMinutes) } +
            STRENGTH_DAYS.mapIndexed { i, day ->
                PlannedSession(
                    day,
                    ExerciseType.Strength,
                    strengthMinutes,
                    StrengthSessions.session(
                        level,
                        second =
                            i > 0,
                    ),
                )
            } +
            PlannedSession(DayOfWeek.WEDNESDAY, ExerciseType.Mobility, MOBILITY_MINUTES)

        return TrainingProgram(
            weekStart = weekStart,
            cardioMinutes = cardioTarget,
            sessions = sessions.sortedBy { it.day },
        )
    }

    /** The Monday of [today]'s week, when every program starts. */
    fun weekStart(today: LocalDate): LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * What the four whole weeks before [weekStart] say about the user: this week's cardio target (at most
     * a tenth more than the recent average, within 90–300 minutes), their favorite cardio and strength level.
     */
    fun summarize(history: List<Exercise>, weekStart: LocalDate): RecentTraining {
        val recent = history.filter { it.date >= weekStart.minusWeeks(HISTORY_WEEKS) && it.date < weekStart }
        val weeklyCardio = recent.filter { it.type.kind == ExerciseKind.Cardio }
            .sumOf { it.duration.toMinutes() } / HISTORY_WEEKS.toDouble()
        val weeklyStrength = recent.count { it.type.kind == ExerciseKind.Strength } / HISTORY_WEEKS.toDouble()
        return RecentTraining(
            cardioTarget = roundTo5(weeklyCardio * PROGRESSION).coerceIn(MIN_CARDIO_MINUTES, MAX_CARDIO_MINUTES),
            cardioType = favoriteCardio(recent),
            strengthPerWeek = weeklyStrength,
            level = StrengthSessions.level(weeklyStrength),
        )
    }

    /** The cardio the user does most, so the program fits their habits. Walking for a new user. */
    private fun favoriteCardio(recent: List<Exercise>): ExerciseType = recent
        .filter { it.type.kind == ExerciseKind.Cardio && it.type != ExerciseType.Other }
        .groupingBy { it.type }
        .eachCount()
        .maxWithOrNull(compareBy<Map.Entry<ExerciseType, Int>> { it.value }.thenBy { -it.key.ordinal })
        ?.key ?: ExerciseType.Walking

    fun roundTo5(minutes: Double): Int = (minutes / 5).roundToInt() * 5
}
