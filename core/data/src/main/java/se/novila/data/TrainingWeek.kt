package se.novila.data

import se.novila.coach.CoachPlan
import se.novila.coach.CoachPlanner
import se.novila.coach.CoachPreferences
import se.novila.coach.SessionFeedback
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseKind
import se.novila.exercise.ExerciseLog
import se.novila.exercise.ExerciseRepository
import se.novila.exercise.ExerciseType
import se.novila.exercise.Progression
import se.novila.exercise.TrainingHistory
import se.novila.exercise.TrainingProgram
import se.novila.exercise.TrainingProgramGenerator
import se.novila.model.CardioType
import se.novila.model.DayEntry
import se.novila.model.TrainingToday
import se.novila.recovery.RecoveryStatus
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

/**
 * The week's training as the app plans it, the same for the Training tab and the training reminder: the coach's
 * [plan] for [preferences], and the [program] that counts (the coach's when it is set up, [active], otherwise generated
 * from recent workouts). [days] are the saved days the [history] was read from, today first. [nextProgram] is the
 * week after, planned the same way on a history that already includes this week, for the weekly review.
 */
class TrainingWeek(
    val days: List<DayEntry>,
    val history: List<Exercise>,
    val preferences: CoachPreferences,
    val active: Boolean,
    val plan: CoachPlan,
    val program: TrainingProgram,
    val nextProgram: TrainingProgram,
) {
    /** Whether the program has a session on [date]'s weekday. */
    fun hasSession(date: LocalDate): Boolean = program.sessions.any { it.day == date.dayOfWeek }

    /**
     * [date]'s training for Today's checklist: the minutes planned that the user can log (mobility is done in their
     * own time, so it never stays open) and the minutes trained, Novila's own and recorded ones counted once.
     */
    fun trainingOn(date: LocalDate): TrainingToday = TrainingToday(
        plannedMinutes = program.sessions
            .filter { it.day == date.dayOfWeek && it.type.kind != ExerciseKind.Mobility }
            .sumOf { it.minutes },
        doneMinutes = history.filter { it.date == date }.sumOf { it.duration.inWholeMinutes }.toInt(),
    )
}

/**
 * Plans the week of [today] from the [saved] days, the coach's [answers] and workouts a watch or another app
 * [recorded]. The same walk can be in both, so [TrainingHistory] counts it once. [draft] are answers the user is
 * trying out before setting up the coach. [recovery] from the watch makes today's session lighter when the body
 * is still recovering, and so does [lighter], when the user chose a lighter session on a day with cycle symptoms.
 * [progress] is what the user did of each exercise lately: the strength sessions build on it a
 * step at a time ([Progression]).
 */
fun trainingWeek(
    saved: HealthStore.SavedDays,
    answers: SavedCoach,
    recorded: List<Exercise>,
    today: LocalDate,
    draft: CoachPreferences? = null,
    recovery: RecoveryStatus = RecoveryStatus.Unknown,
    progress: ExerciseLog = ExerciseLog(),
    lighter: Boolean = false,
): TrainingWeek {
    // Strength sessions done in session mode and cardio logged by hand, together with what Health Connect recorded.
    val days = saved.lastDays(count = ExerciseRepository.HISTORY_DAYS.toInt(), today = today)
    val history = TrainingHistory.merge(days.flatMap(::ownExercises), recorded)
    val feedback = days.mapNotNull(::feedback)
    val active = answers.preferences
    val preferences = active ?: draft ?: CoachPreferences()
    val planned = CoachPlanner.plan(
        preferences,
        history = history,
        today = today,
        feedback = feedback,
        recovery = recovery,
        lighter = lighter,
    )
    val plan = planned.copy(program = Progression.apply(planned.program, progress, today))
    val program = if (active != null) {
        plan.program
    } else {
        val generated = TrainingProgramGenerator.generate(history, today, if (lighter) RecoveryStatus.Low else recovery)
        Progression.apply(generated, progress, today)
    }
    // Next week from the same coach or generator, on a history where this week's sessions already count,
    // so the weekly review can say what the training leads to. Recovery and a lighter day only lighten today.
    val nextMonday = program.weekStart.plusWeeks(1)
    val nextProgram = Progression.apply(
        if (active != null) {
            CoachPlanner.plan(preferences, history = history, today = nextMonday, feedback = feedback).program
        } else {
            TrainingProgramGenerator.generate(history, nextMonday)
        },
        progress,
        nextMonday,
    )
    return TrainingWeek(days, history, preferences, active != null, plan, program, nextProgram)
}

private fun ownExercises(day: DayEntry): List<Exercise> = buildList {
    if (day.strengthMinutes > 0) add(Exercise(ExerciseType.Strength, day.date, day.strengthMinutes.minutes))
    day.cardio.forEach { add(Exercise(it.type.exerciseType, day.date, it.minutes.minutes)) }
}

private fun feedback(day: DayEntry): SessionFeedback? {
    if (day.effort == 0 && day.pain == null) return null
    return SessionFeedback(day.date, day.effort.takeIf { it > 0 }, day.pain, day.rehabPhase)
}

/** The training program's type for cardio logged by hand. */
val CardioType.exerciseType: ExerciseType
    get() = when (this) {
        CardioType.Walking -> ExerciseType.Walking
        CardioType.Running -> ExerciseType.Running
        CardioType.Cycling -> ExerciseType.Cycling
        CardioType.Swimming -> ExerciseType.Swimming
    }

/** The cardio type to log for a planned [type], or null when it isn't one the user logs by hand. */
val ExerciseType.cardioType: CardioType?
    get() = CardioType.entries.firstOrNull { it.exerciseType == this }
