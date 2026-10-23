package se.novila.exercise

import se.novila.recovery.RecoveryStatus
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

/**
 * The user's recent training, which a program builds on. [cardioTarget] is this week's cardio in moderate minutes, as
 * WHO counts it ([Intensity]); [cardioMinutes] turns it into minutes of the user's favorite cardio. [strengthPerWeek]
 * is sessions a week.
 */
data class RecentTraining(
    val cardioTarget: Int,
    val cardioType: ExerciseType,
    val strengthPerWeek: Double,
    val level: StrengthLevel,
) {
    /** Minutes of [cardioType] for [moderateMinutes] of cardio as WHO counts it, e.g. half as many for running. */
    fun cardioMinutes(moderateMinutes: Double): Double = moderateMinutes / cardioType.intensity.moderateMinutes
}

/**
 * A week of workouts starting on Monday [weekStart], sorted by day. [cardioMinutes] are minutes on the clock. In a
 * [lighterWeek] the strength sessions have a set less and hold their targets ([Progression.lighterWeek]).
 */
data class TrainingProgram(
    val weekStart: LocalDate,
    val cardioMinutes: Int,
    val sessions: List<PlannedSession>,
    val lighterWeek: Boolean = false,
)

/**
 * Builds a week's program from recent workouts, along WHO's guidelines for adults: 150–300 minutes of
 * moderate cardio (or half as much vigorous, see [Intensity]) and strength training on at least two days a week. It
 * starts where the user is and raises the cardio by at most 10 % a week, so a beginner gets a gentle program and an
 * active user isn't held back. Strength days get concrete exercises from [StrengthSessions] at the user's level.
 * When the body is still recovering ([RecoveryStatus.Low]), today's session alone is lighter, a day at a
 * time: the watch's resting heart rate and HRV are what the status comes from. Plain Kotlin, so it is
 * unit tested; the source of the workouts doesn't matter.
 */
object TrainingProgramGenerator {
    const val MIN_CARDIO_MINUTES = 90
    const val MAX_CARDIO_MINUTES = 300
    const val GUIDELINE_CARDIO_MINUTES = 150
    private const val HISTORY_WEEKS = 4L
    private const val PROGRESSION = 1.1
    private const val MOBILITY_MINUTES = 15
    private const val RECOVERY_CARDIO_FACTOR = 0.8
    private const val RECOVERY_STRENGTH_FACTOR = 0.7
    private const val RECOVERY_MIN_CARDIO_MINUTES = 10
    private const val RECOVERY_MIN_STRENGTH_MINUTES = 15
    private val STRENGTH_DAYS = listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
    private val THREE_CARDIO_DAYS = listOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
    private val FOUR_CARDIO_DAYS = listOf(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun generate(
        history: List<Exercise>,
        today: LocalDate,
        recovery: RecoveryStatus = RecoveryStatus.Unknown,
    ): TrainingProgram {
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

        val cardioMinutes = roundTo5(recent.cardioMinutes(cardioTarget / cardioDays.size.toDouble()))
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
            cardioMinutes = roundTo5(recent.cardioMinutes(cardioTarget.toDouble())),
            sessions = sessions.map { session ->
                if (session.day == today.dayOfWeek) session.lowered(recovery) else session
            }.sortedBy { it.day },
        )
    }

    /**
     * [session] as today's when the body is still recovering: a little less cardio, and a shorter strength session
     * with one set less of each exercise, so session mode does what the shorter time says.
     */
    private fun PlannedSession.lowered(recovery: RecoveryStatus): PlannedSession =
        if (recovery != RecoveryStatus.Low) {
            this
        } else {
            when (type.kind) {
                ExerciseKind.Cardio -> copy(
                    minutes = roundTo5(minutes * RECOVERY_CARDIO_FACTOR).coerceAtLeast(RECOVERY_MIN_CARDIO_MINUTES),
                )

                ExerciseKind.Strength -> copy(
                    minutes = roundTo5(minutes * RECOVERY_STRENGTH_FACTOR).coerceAtLeast(RECOVERY_MIN_STRENGTH_MINUTES),
                    exercises = exercises.map { it.copy(sets = (it.sets - 1).coerceAtLeast(1)) },
                )

                ExerciseKind.Mobility -> this
            }
        }

    /** The Monday of [today]'s week, when every program starts. */
    fun weekStart(today: LocalDate): LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * What the four whole weeks before [weekStart] say about the user: this week's cardio target in moderate minutes
     * (at most a tenth more than the recent average, within 90–300), their favorite cardio and strength level.
     */
    fun summarize(history: List<Exercise>, weekStart: LocalDate): RecentTraining {
        val recent = history.filter { it.date >= weekStart.minusWeeks(HISTORY_WEEKS) && it.date < weekStart }
        val weeklyCardio = recent.filter { it.type.kind == ExerciseKind.Cardio }
            .sumOf { it.moderateMinutes } / HISTORY_WEEKS.toDouble()
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
