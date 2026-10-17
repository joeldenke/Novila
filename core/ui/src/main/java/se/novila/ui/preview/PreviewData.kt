package se.novila.ui.preview

import se.novila.model.DayEntry
import se.novila.model.FoodLog
import se.novila.model.GOOD_NUTRITION_SCORE
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.Profile
import se.novila.model.SLEEP_WINDOW_NIGHTS
import se.novila.model.SleepFactor
import se.novila.model.historyOf
import se.novila.model.nutritionScore
import se.novila.movement.MovementState
import se.novila.movement.TodayMovement
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

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

    /** Who the previews are about. */
    val profile = Profile(name = "Alex", birthYear = 1990, heightCm = 178, weightKg = 74)

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

    /**
     * Any day up to [date]: the week above, and before it the same rhythm repeated, with a night or a day's food
     * forgotten now and then and a little less sleep and more treats further back, so month and year charts show a
     * trend. The earlier nights have bed and wake times, how they felt and what affected them, with links built in
     * for the sleep findings: about 45 minutes more and a better night after a day eaten well, as much less and a
     * worse night after late caffeine.
     */
    fun dayOn(date: LocalDate): DayEntry {
        val daysAgo = ChronoUnit.DAYS.between(date, this.date).toInt()
        if (daysAgo < 0) return DayEntry(date)
        if (daysAgo == 0) return today
        if (daysAgo < 7) return history[daysAgo - 1]
        val like = history[daysAgo % history.size]
        val day = like.copy(date = date, food = foodOn(daysAgo), sleepHours = 0f)
        if (daysAgo % 11 == 0) return day
        val dayBefore = DayEntry(date, waterGlasses = waterOn(daysAgo + 1), food = foodOn(daysAgo + 1))
        val ateWellBefore = (dayBefore.nutritionScore()?.total ?: 0) >= GOOD_NUTRITION_SCORE
        val caffeine = daysAgo % 4 == 1
        val effect = (if (ateWellBefore) 1 else 0) - (if (caffeine) 1 else 0)
        val earlier = daysAgo / 60 // two months at a time
        val minutes = ((like.sleepHours - earlier * 0.25f + effect * 0.75f).coerceIn(5f, 10f) * 60).toLong()
        val bedtime = LocalTime.of(22, 45).plusMinutes(((daysAgo % 5) - 2) * 20L)
        return day.withSleepTimes(bedtime, bedtime.plusMinutes(minutes))
            .withSleepQuality(3 + effect)
            .withSleepFactor(SleepFactor.LateCaffeine, caffeine)
            .withSleepFactor(SleepFactor.Screens, daysAgo % 6 == 2)
    }

    /** What was eaten [daysAgo] days before [date]: the week above, then its rhythm with more treats further back. */
    private fun foodOn(daysAgo: Int): FoodLog = when {
        daysAgo == 0 -> today.food
        daysAgo < 7 -> history[daysAgo - 1].food
        daysAgo % 13 == 0 -> FoodLog()
        else -> history[daysAgo % history.size].food.let { it.copy(treats = it.treats + daysAgo / 60 / 2) }
    }

    private fun waterOn(daysAgo: Int): Int = when {
        daysAgo == 0 -> today.waterGlasses
        daysAgo < 7 -> history[daysAgo - 1].waterGlasses
        else -> history[daysAgo % history.size].waterGlasses
    }

    /** [count] days up to [until], oldest first, from [dayOn]: enough for the sleep findings by default. */
    fun days(until: LocalDate = date, count: Int = SLEEP_WINDOW_NIGHTS + 1): List<DayEntry> =
        (count - 1 downTo 0).map { dayOn(until.minusDays(it.toLong())) }

    /** The history over [range] up to [until], from [dayOn]. */
    fun history(range: HistoryRange, until: LocalDate = date): History = historyOf(range, until, ::dayOn)
}
