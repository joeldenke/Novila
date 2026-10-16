package se.novila.ui.preview

import se.novila.CoachUiState
import se.novila.DayEntry
import se.novila.FoodLog
import se.novila.coach.BodyArea
import se.novila.coach.CoachGoal
import se.novila.coach.CoachPlanner
import se.novila.coach.CoachPreferences
import se.novila.coach.Consideration
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseType
import se.novila.exercise.TrainingProgramGenerator
import se.novila.movement.MovementState
import se.novila.movement.TodayMovement
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate

/** Fixed sample data for Compose previews: a realistic week with one rough day early on. */
internal object PreviewData {
    private val date: LocalDate = LocalDate.of(2026, 10, 14)

    private fun day(
        daysAgo: Long,
        sleep: Float,
        water: Int,
        mood: Int,
        food: FoodLog,
    ) = DayEntry(date.minusDays(daysAgo), waterGlasses = water, sleepHours = sleep, mood = mood, food = food)

    val today = day(0, sleep = 7.5f, water = 5, mood = 4, food = FoodLog(3, 2, 2, 1))

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

    /** The coach before it is set up. */
    val newCoach = coach(CoachPreferences(), active = false)

    val movement = MovementState.Connected(TodayMovement(steps = 8412, activeKilocalories = 312))

    /** Newest first, like HealthUiState.history. Food: vegetables, meals, protein, whole grain, treats. */
    val history: List<DayEntry> = listOf(
        day(1, sleep = 8.5f, water = 8, mood = 5, food = FoodLog(5, 3, 3, 3)),
        day(2, sleep = 6.5f, water = 6, mood = 3, food = FoodLog(3, 3, 2, 2, 2)),
        day(3, sleep = 7.5f, water = 7, mood = 4, food = FoodLog(4, 3, 3, 3, 1)),
        day(4, sleep = 8f, water = 8, mood = 5, food = FoodLog(5, 3, 3, 3)),
        day(5, sleep = 7f, water = 7, mood = 4, food = FoodLog(4, 3, 3, 2, 1)),
        day(6, sleep = 6f, water = 4, mood = 2, food = FoodLog(2, 2, 1, 1, 3)),
    )
}
