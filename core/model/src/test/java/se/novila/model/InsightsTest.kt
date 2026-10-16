package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsightsTest {
    private val today = LocalDate.of(2026, 10, 15)
    private val goodFood = FoodLog(vegetables = 5, meals = 3, protein = 3, wholeGrain = 3)
    private val poorFood = FoodLog(vegetables = 1, meals = 2)

    /** One entry per day, oldest first, ending today. */
    private fun week(vararg days: DayEntry.() -> DayEntry): List<DayEntry> =
        days.mapIndexed { i, change -> DayEntry(today.minusDays((days.size - 1 - i).toLong())).change() }

    @Test fun `an empty week has nothing to say`() {
        assertTrue(weekInsights(week({ this }, { this }, { this }), steps = null).isEmpty())
    }

    @Test fun `week runs from oldest to today`() {
        val history = listOf(DayEntry(today.minusDays(1)), DayEntry(today.minusDays(2)))
        val daysAgo = weekOf(DayEntry(today), history).map { today.toEpochDay() - it.date.toEpochDay() }
        assertEquals(listOf(2L, 1L, 0L), daysAgo)
    }

    @Test fun `mood that follows sleep is found`() {
        val days = week(
            { copy(sleepHours = 6f, mood = 2) },
            { copy(sleepHours = 8f, mood = 5) },
            { copy(sleepHours = 6.5f, mood = 2) },
            { copy(sleepHours = 7.5f, mood = 4) },
        )
        assertTrue(Insight.MoodFollowsSleep(MOOD_SLEEP_HOURS) in weekInsights(days, steps = null))
    }

    @Test fun `mood needs two days on each side`() {
        val days = week({ copy(sleepHours = 6f, mood = 1) }, { copy(sleepHours = 8f, mood = 5) })
        assertTrue(weekInsights(days, steps = null).none { it is Insight.MoodFollowsSleep })
    }

    @Test fun `last night is compared with the week`() {
        val days = week(
            { copy(sleepHours = 7f) },
            { copy(sleepHours = 7f) },
            { copy(sleepHours = 7f) },
            { copy(sleepHours = 8.5f) },
        )
        assertTrue(Insight.SleepVsAverage(1.5f) in weekInsights(days, steps = null))
    }

    @Test fun `steady sleep and the step goal are noticed`() {
        val days = week(
            { copy(sleepHours = 7f) },
            { copy(sleepHours = 7.5f) },
            { copy(sleepHours = 8f) },
            { copy(sleepHours = 7f) },
        )
        val insights = weekInsights(days, steps = STEPS_GOAL, limit = 10)
        assertTrue(Insight.SteadySleep in insights)
        assertTrue(Insight.StepsGoalReached in insights)
    }

    @Test fun `a streak of good days counts from yesterday while today is in progress`() {
        val days = week({ copy(food = goodFood) }, { copy(food = goodFood) }, { copy(food = goodFood) }, { this })
        assertTrue(Insight.NutritionStreak(3) in weekInsights(days, steps = null))
    }

    @Test fun `nutrition trend compares the last three days with the ones before`() {
        val days = week(
            { copy(food = poorFood) },
            { copy(food = poorFood) },
            { copy(food = goodFood) },
            { copy(food = goodFood) },
            { copy(food = goodFood) },
            { this },
        )
        val trend = weekInsights(days, steps = null, limit = 10).filterIsInstance<Insight.NutritionTrend>().single()
        assertTrue(trend.points > 0)
    }

    @Test fun `at most three insights by default`() {
        val days = week(
            { copy(sleepHours = 6f, mood = 2, food = goodFood) },
            { copy(sleepHours = 6f, mood = 2, food = goodFood) },
            { copy(sleepHours = 7f, mood = 5, food = goodFood) },
            { copy(sleepHours = 8.5f, mood = 5, food = goodFood) },
        )
        assertEquals(3, weekInsights(days, steps = STEPS_GOAL).size)
    }
}
