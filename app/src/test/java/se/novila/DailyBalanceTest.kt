package se.novila

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DailyBalanceTest {
    private val date = LocalDate.of(2026, 10, 15)
    private val allFood = FoodLog(vegetables = 5, meals = 3, protein = 3, wholeGrain = 3)

    @Test fun `balance weighs sleep and nutrition equally without movement`() {
        assertEquals(0, DayEntry(date).balance(steps = null))
        assertEquals(50, DayEntry(date, sleepHours = SLEEP_GOAL_HOURS).balance(steps = null))
        assertEquals(100, DayEntry(date, waterGlasses = WATER_GOAL, sleepHours = 11f, food = allFood).balance(null))
    }

    @Test fun `movement counts as a third part once it is known`() {
        val day = DayEntry(date, waterGlasses = WATER_GOAL, sleepHours = SLEEP_GOAL_HOURS, food = allFood)
        assertEquals(100, day.balance(steps = STEPS_GOAL))
        assertEquals(83, day.balance(steps = STEPS_GOAL / 2))
        assertEquals(67, day.balance(steps = 0))
    }

    @Test fun `tip asks for sleep first, then the most valuable portion`() {
        assertEquals(DayTip.LogSleep, DayEntry(date, waterGlasses = 3).tip())
        val noMeals = DayEntry(date, sleepHours = 7f, food = allFood.copy(meals = 0))
        assertEquals(DayTip.Eat(NextPortion(FoodGroup.Meals, 7)), noMeals.tip())
        val done = DayEntry(date, waterGlasses = WATER_GOAL, sleepHours = 7f, food = allFood)
        assertEquals(DayTip.GoalsMet, done.tip())
    }

    @Test fun `greeting follows the time of day`() {
        assertEquals(Greeting.Evening, greetingFor(4))
        assertEquals(Greeting.Morning, greetingFor(5))
        assertEquals(Greeting.Morning, greetingFor(11))
        assertEquals(Greeting.Afternoon, greetingFor(12))
        assertEquals(Greeting.Afternoon, greetingFor(17))
        assertEquals(Greeting.Evening, greetingFor(18))
    }
}
