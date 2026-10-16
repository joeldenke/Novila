package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalsTest {
    private val date = LocalDate.of(2026, 10, 16)

    @Test fun `the defaults are the general advice`() {
        assertEquals(Goals(steps = 8000, sleepHours = 8f, waterGlasses = 8), Goals())
    }

    @Test fun `goals change in steps and stay within their ranges`() {
        assertEquals(8500, Goals().withSteps(+1).steps)
        assertEquals(Goals.STEPS.first, Goals(steps = 2000).withSteps(-1).steps)
        assertEquals(Goals.STEPS.last, Goals(steps = 20000).withSteps(+1).steps)
        assertEquals(7.5f, Goals().withSleep(-1).sleepHours)
        assertEquals(10f, Goals(sleepHours = 10f).withSleep(+1).sleepHours)
        assertEquals(4, Goals(waterGlasses = 4).withWater(-1).waterGlasses)
    }

    @Test fun `a broken saved goal is moved into its range`() {
        assertEquals(Goals(steps = 2000, sleepHours = 6f, waterGlasses = 4), Goals(0, 0f, 0).clamped())
        assertEquals(7.5f, Goals(sleepHours = 7.4f).clamped().sleepHours)
    }

    @Test fun `suggestions follow age and weight`() {
        assertEquals(Goals(), Goals.suggested(Profile(), 2026))
        assertEquals(Goals(waterGlasses = 8), Goals.suggested(Profile(birthYear = 1990, weightKg = 70), 2026))
        assertEquals(Goals(steps = 7000, sleepHours = 7.5f), Goals.suggested(Profile(birthYear = 1950), 2026))
        assertEquals(9f, Goals.suggested(Profile(birthYear = 2010), 2026).sleepHours)
        assertEquals(6, Goals.suggested(Profile(weightKg = 40), 2026).waterGlasses)
        assertEquals(10, Goals.suggested(Profile(weightKg = 120), 2026).waterGlasses)
    }

    @Test fun `a suggestion needs age or weight`() {
        assertFalse(Goals.canSuggest(Profile(name = "Anna", heightCm = 168)))
        assertTrue(Goals.canSuggest(Profile(weightKg = 64)))
    }

    @Test fun `progress and balance are measured against the user's goals`() {
        val goals = Goals(steps = 10000, sleepHours = 7f, waterGlasses = 4)
        assertEquals(0.5f, stepsProgress(5000, goals))
        assertEquals(1f, DayEntry(date, sleepHours = 7f).sleepProgress(goals))
        val food = FoodLog(vegetables = 5, meals = 3, protein = 3, wholeGrain = 3)
        val day = DayEntry(date, waterGlasses = 4, sleepHours = 7f, food = food)
        assertEquals(100, day.balance(steps = 10000, goals = goals))
        assertNull(day.nextPortion(goals))
        assertEquals(NextPortion(FoodGroup.Water, 2), day.nextPortion())
    }

    @Test fun `the step insight names the user's goal`() {
        val goals = Goals(steps = 6000)
        val week = listOf(DayEntry(date))
        assertTrue(Insight.StepsGoalReached(6000) in weekInsights(week, steps = 6500, goals = goals))
        assertTrue(weekInsights(week, steps = 6500).none { it is Insight.StepsGoalReached })
    }
}
