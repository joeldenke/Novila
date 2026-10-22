package se.novila.feature.training

import se.novila.coach.CoachPlan
import se.novila.coach.CoachPreferences
import se.novila.exercise.TrainingProgram
import se.novila.exercise.Workout
import se.novila.model.CardioLog
import se.novila.recovery.RecoveryStatus
import java.time.DayOfWeek

/** What the training screens show for one day. */
data class TrainingUiState(
    /** This week's workouts: the coach's when it is set up, otherwise generated from recent ones. */
    val program: TrainingProgram,
    val coach: CoachUiState,
    val today: DayOfWeek,
    val strengthMinutesToday: Int,
    /** How the body seems to have recovered, which today's session may already be lighter for. */
    val recovery: RecoveryStatus = RecoveryStatus.Unknown,
    val cardioToday: CardioToday = CardioToday(),
    /** Minutes trained on each day of this week so far, Novila's own sessions and recorded ones counted once. */
    val weekDone: Map<DayOfWeek, Int> = emptyMap(),
    /** Next week's program, planned the same way on a history that includes this week, for the weekly review. */
    val nextProgram: TrainingProgram? = null,
)

/** Today's cardio: what the user [logged] by hand and the minutes a watch or another app recorded. */
data class CardioToday(val logged: List<CardioLog> = emptyList(), val recordedMinutes: Int = 0) {
    val loggedMinutes: Int get() = logged.sumOf { it.minutes }
}

/** What the user says after a strength session: effort (RPE 1–10) and pain (0–10), null until answered. */
data class SessionFeedbackDraft(val effort: Int? = null, val pain: Int? = null)

/**
 * The coach's [preferences] (the defaults before it is set up) and the [plan] they give. [active] when the
 * user has set it up, so the plan is the week's program.
 */
data class CoachUiState(val preferences: CoachPreferences, val plan: CoachPlan, val active: Boolean)

/**
 * A strength session in progress, with the seconds left of a timed step (a rest or a hold), and once it is done what
 * the user says about it. A [rehab] session is dosed by pain, so it neither asks for repetitions nor raises them.
 */
data class WorkoutUiState(
    val workout: Workout,
    val secondsLeft: Int?,
    val feedback: SessionFeedbackDraft = SessionFeedbackDraft(),
    val rehab: Boolean = false,
)
