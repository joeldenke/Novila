package se.novila

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DailyBalanceTest {
    private val date = LocalDate.of(2026, 10, 15)

    @Test fun `balance weighs water and sleep equally`() {
        assertEquals(0, DayEntry(date).balance)
        assertEquals(50, DayEntry(date, waterGlasses = WATER_GOAL).balance)
        assertEquals(75, DayEntry(date, waterGlasses = WATER_GOAL / 2, sleepHours = SLEEP_GOAL_HOURS).balance)
        assertEquals(100, DayEntry(date, waterGlasses = WATER_GOAL * 2, sleepHours = 11f).balance)
    }

    @Test fun `tip asks for sleep first, then water`() {
        assertEquals(DayTip.LogSleep, DayEntry(date, waterGlasses = 3).tip())
        assertEquals(DayTip.DrinkWater(5), DayEntry(date, waterGlasses = 3, sleepHours = 7f).tip())
        assertEquals(DayTip.GoalsMet, DayEntry(date, waterGlasses = WATER_GOAL, sleepHours = 7f).tip())
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
