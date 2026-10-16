package se.novila.ui.preview

import se.novila.model.DayEntry
import se.novila.model.FoodLog
import se.novila.movement.MovementState
import se.novila.movement.TodayMovement
import java.time.LocalDate

/**
 * Fixed sample data for Compose previews: a realistic week with one rough day early on. Shared by every feature,
 * so their previews (and screenshot tests) show the same person. Feature-specific samples live in the feature.
 */
object PreviewData {
    /** The day every preview is set on. */
    val date: LocalDate = LocalDate.of(2026, 10, 14)

    private fun day(
        daysAgo: Long,
        sleep: Float,
        water: Int,
        mood: Int,
        food: FoodLog,
    ) = DayEntry(date.minusDays(daysAgo), waterGlasses = water, sleepHours = sleep, mood = mood, food = food)

    val today = day(0, sleep = 7.5f, water = 5, mood = 4, food = FoodLog(3, 2, 2, 1))

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
