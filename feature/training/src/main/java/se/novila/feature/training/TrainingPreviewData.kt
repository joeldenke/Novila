package se.novila.feature.training

import se.novila.anatomy.Equipment
import se.novila.coach.AssistantAvailability
import se.novila.coach.BodyArea
import se.novila.coach.CoachGoal
import se.novila.coach.CoachPlanner
import se.novila.coach.CoachPreferences
import se.novila.coach.Consideration
import se.novila.coach.RehabArea
import se.novila.coach.SessionFeedback
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseType
import se.novila.exercise.TrainingProgramGenerator
import se.novila.recovery.RecoveryDay
import se.novila.recovery.RecoveryState
import se.novila.ui.preview.PreviewData
import java.time.DayOfWeek
import kotlin.time.Duration.Companion.minutes

/** Training samples for Compose previews, on the same day as [PreviewData]. */
internal object TrainingPreviewData {
    private val date = PreviewData.date

    /** Someone who runs a little and has just started with strength training. */
    val workouts = (1L..4L).flatMap { week ->
        val monday = date.minusWeeks(week).with(DayOfWeek.MONDAY)
        listOf(
            Exercise(ExerciseType.Running, monday.plusDays(1), 35.minutes),
            Exercise(ExerciseType.Running, monday.plusDays(5), 50.minutes),
            Exercise(ExerciseType.Strength, monday.plusDays(3), 30.minutes),
        )
    }

    val program = TrainingProgramGenerator.generate(history = workouts, today = date)

    /** The week after, as the weekly review shows it. */
    val nextProgram = TrainingProgramGenerator.generate(history = workouts, today = date.plusWeeks(1))

    /** The watch's recovery: a raised resting heart rate and a lower HRV today, so today is lighter. */
    val recovery = RecoveryState.Connected(
        days = (0L..6L).map { daysAgo ->
            RecoveryDay(
                date = date.minusDays(daysAgo),
                restingHeartRate = if (daysAgo == 0L) 56 else 50,
                hrvMillis = if (daysAgo == 0L) 38 else 50,
            )
        }.sortedBy { it.date },
    )

    private fun coach(preferences: CoachPreferences, active: Boolean, feedback: List<SessionFeedback> = emptyList()) =
        CoachUiState(preferences, CoachPlanner.plan(preferences, workouts, date, feedback), active)

    /** The coach set up for strength, with sensitive knees and extra leg work. */
    val coach = coach(
        CoachPreferences(
            goal = CoachGoal.Strength,
            daysPerWeek = 4,
            sessionMinutes = 30,
            focus = setOf(BodyArea.Legs),
            considerations = setOf(Consideration.Knees),
            equipment = setOf(Equipment.Dumbbells),
        ),
        active = true,
        // The last two sessions felt 6 and 7 of 10: the dose stays.
        feedback = listOf(
            SessionFeedback(date.minusDays(2), effort = 6, pain = 0),
            SessionFeedback(date.minusDays(5), effort = 7, pain = 1),
        ),
    )

    /** A rehab program for the lower back, in its second phase, after three calm sessions: the coach suggests phase 3. */
    val rehabCoach = coach(
        CoachPreferences(goal = CoachGoal.Rehab, daysPerWeek = 5, rehabArea = RehabArea.LowerBack, rehabPhase = 2),
        active = true,
        feedback = listOf(1L, 3L, 5L).map { daysAgo ->
            SessionFeedback(date.minusDays(daysAgo), effort = 5, pain = 2, rehabPhase = 2)
        },
    )

    /** What Gemini Nano might write about [coach]'s week. */
    val assistantMessage = AssistantUiState(
        availability = AssistantAvailability.Available,
        message = "Den här veckan bygger du styrka i benen tre dagar, med vila emellan så att musklerna " +
            "hinner återhämta sig. Knäböj och höftlyft gör jobbet utan hopp, så knäna skonas. Börja " +
            "måndagens pass lugnt och fokusera på tekniken i första setet.",
    )

    /** The coach before it is set up. */
    val newCoach = coach(CoachPreferences(), active = false)
}
