package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HistoryTest {
    private val today = LocalDate.of(2026, 10, 17)

    private fun empty(date: LocalDate) = DayEntry(date)

    @Test fun `a week is a point per day, ending on the shown day`() {
        val history = historyOf(HistoryRange.Week, today, ::empty)
        assertEquals(7, history.periods.size)
        assertTrue(history.periods.all { it.days.size == 1 })
        assertEquals(today.minusDays(6), history.periods.first().start)
        assertEquals(today, history.days.last().date)
        assertEquals((1L..7L).map { today.minusDays(6 + it) }.reversed(), history.previous.map { it.date })
    }

    @Test fun `a month is four weeks of seven days`() {
        val history = historyOf(HistoryRange.Month, today, ::empty)
        assertEquals(listOf(7, 7, 7, 7), history.periods.map { it.days.size })
        assertEquals(today.minusDays(27), history.periods.first().start)
        assertEquals(today, history.days.last().date)
        assertEquals(28, history.previous.size)
        assertEquals(today.minusDays(28), history.previous.last().date)
    }

    @Test fun `a year is twelve calendar months, the newest up to the shown day`() {
        val history = historyOf(HistoryRange.Year, today, ::empty)
        assertEquals(12, history.periods.size)
        assertEquals(LocalDate.of(2025, 11, 1), history.periods.first().start)
        assertEquals(30, history.periods.first().days.size)
        assertEquals(LocalDate.of(2026, 10, 1), history.periods.last().start)
        assertEquals(17, history.periods.last().days.size)
        // Every day once, without gaps.
        val dates = history.days.map { it.date }
        assertEquals(dates.distinct(), dates)
        assertEquals(dates.first().plusDays(dates.size - 1L), today)
        assertEquals(dates.size, history.previous.size)
    }

    @Test fun `the trend compares the range with the one before it`() {
        val history = historyOf(HistoryRange.Month, today) { date ->
            val recent = date > today.minusDays(28)
            DayEntry(
                date,
                sleepHours = if (recent) 7.5f else 6.5f,
                food = if (recent) FoodLog(5, 3, 3, 3) else FoodLog(2, 2, 1, 1, 3),
            )
        }
        assertEquals(1f, history.sleepTrend())
        val nutrition = history.nutritionTrend()!!
        assertTrue(nutrition > 0)
        assertEquals(
            FoodLog(5, 3, 3, 3).let { DayEntry(today, food = it) }.nutritionScore()!!.total -
                DayEntry(today, food = FoodLog(2, 2, 1, 1, 3)).nutritionScore()!!.total,
            nutrition,
        )
    }

    @Test fun `no trend without logs on both sides`() {
        val history = historyOf(HistoryRange.Month, today) { date ->
            DayEntry(date, sleepHours = if (date > today.minusDays(28)) 7f else 0f)
        }
        assertNull(history.sleepTrend())
        assertNull(history.nutritionTrend())
    }

    @Test fun `a week from a list has nothing to compare with`() {
        val week = (6L downTo 0L).map { DayEntry(today.minusDays(it), sleepHours = 7f) }
        val history = historyOfWeek(week)
        assertEquals(week, history.days)
        assertNull(history.sleepTrend())
    }
}
