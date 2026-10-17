package se.novila.feature.training

import se.novila.coach.AssistantAvailability
import se.novila.coach.CoachPlan
import se.novila.coach.CoachPreferences
import se.novila.exercise.TrainingProgram
import se.novila.exercise.Workout
import se.novila.model.CardioLog
import java.time.DayOfWeek

/** What the training screens show for one day. */
data class TrainingUiState(
    /** This week's workouts: the coach's when it is set up, otherwise generated from recent ones. */
    val program: TrainingProgram,
    val coach: CoachUiState,
    val today: DayOfWeek,
    val strengthMinutesToday: Int,
    val cardioToday: CardioToday = CardioToday(),
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
 * The coach's message from the on-device model: whether the model can be used, whether it is [writing], and the
 * [message] it wrote for the current plan. [failed] when the last attempt gave nothing.
 */
data class AssistantUiState(
    val availability: AssistantAvailability = AssistantAvailability.Unknown,
    val writing: Boolean = false,
    val message: String? = null,
    val failed: Boolean = false,
)

/**
 * A strength session in progress, with the seconds left of a timed step (a rest or a hold), and once it is done what
 * the user says about it.
 */
data class WorkoutUiState(
    val workout: Workout,
    val secondsLeft: Int?,
    val feedback: SessionFeedbackDraft = SessionFeedbackDraft(),
)
