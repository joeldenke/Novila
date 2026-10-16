package se.novila.feature.training

import se.novila.coach.AssistantAvailability
import se.novila.coach.BodyArea
import se.novila.coach.CoachGoal
import se.novila.coach.CoachPlanner
import se.novila.coach.CoachPreferences
import se.novila.coach.Consideration
import se.novila.coach.RehabArea
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseType
import se.novila.exercise.TrainingProgramGenerator
import se.novila.ui.preview.PreviewData
import java.time.DayOfWeek
import java.time.Duration

/** Training samples for Compose previews, on the same day as [PreviewData]. */
internal object TrainingPreviewData {
    private val date = PreviewData.date

    /** Someone who runs a little and has just started with strength training. */
    private val workouts = (1L..4L).flatMap { week ->
        val monday = date.minusWeeks(week).with(DayOfWeek.MONDAY)
        listOf(
            Exercise(ExerciseType.Running, monday.plusDays(1), Duration.ofMinutes(35)),
            Exercise(ExerciseType.Running, monday.plusDays(5), Duration.ofMinutes(50)),
            Exercise(ExerciseType.Strength, monday.plusDays(3), Duration.ofMinutes(30)),
        )
    }

    val program = TrainingProgramGenerator.generate(history = workouts, today = date)

    private fun coach(preferences: CoachPreferences, active: Boolean) =
        CoachUiState(preferences, CoachPlanner.plan(preferences, workouts, date), active)

    /** The coach set up for strength, with sensitive knees and extra leg work. */
    val coach = coach(
        CoachPreferences(
            goal = CoachGoal.Strength,
            daysPerWeek = 4,
            sessionMinutes = 30,
            focus = setOf(BodyArea.Legs),
            considerations = setOf(Consideration.Knees),
        ),
        active = true,
    )

    /** A rehab program for the lower back, in its second phase. */
    val rehabCoach = coach(
        CoachPreferences(goal = CoachGoal.Rehab, daysPerWeek = 5, rehabArea = RehabArea.LowerBack, rehabPhase = 2),
        active = true,
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
