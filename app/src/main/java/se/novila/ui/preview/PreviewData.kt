package se.novila.ui.preview

import se.novila.DayEntry
import se.novila.FoodLog
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

    /** A program for someone who runs a little and has just started with strength training. */
    val program = TrainingProgramGenerator.generate(
        history = (1L..4L).flatMap { week ->
            val monday = date.minusWeeks(week).with(DayOfWeek.MONDAY)
            listOf(
                Exercise(ExerciseType.Running, monday.plusDays(1), Duration.ofMinutes(35)),
                Exercise(ExerciseType.Running, monday.plusDays(5), Duration.ofMinutes(50)),
                Exercise(ExerciseType.Strength, monday.plusDays(3), Duration.ofMinutes(30)),
            )
        },
        today = date,
    )

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
