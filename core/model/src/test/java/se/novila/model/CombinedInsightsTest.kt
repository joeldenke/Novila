package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CombinedInsightsTest {
    private val today = LocalDate.of(2026, 10, 17)
    private val good = FoodLog(vegetables = 5, meals = 3, protein = 3, wholeGrain = 3)
    private val poor = FoodLog(vegetables = 1, meals = 2, protein = 1, wholeGrain = 0, treats = 3)

    /** [count] days ending today, oldest first, each made by [day] from its index (0 is the oldest). */
    private fun days(count: Int, day: (Int, LocalDate) -> DayEntry): List<DayEntry> =
        (0 until count).map { i -> day(i, today.minusDays((count - 1 - i).toLong())) }

    /** A little variation, so the groups aren't flat: -0.25, 0, +0.25 in turn. */
    private fun wobble(i: Int) = ((i % 3) - 1) * 0.25f

    @Test fun `a full night shows in more training that day, with the days behind it`() {
        val days = days(40) { i, date ->
            val rested = i % 2 == 0
            DayEntry(date, strengthMinutes = if (rested) 40 else 10)
                .copy(sleepHours = (if (rested) 8.5f else 6.5f) + wobble(i))
        }
        val connection = connections(days).filterIsInstance<TrainingAfterSleep>().single()
        assertEquals(30.0, connection.difference, 3.0)
        assertTrue(connection.withDays >= MIN_NIGHTS && connection.withoutDays >= MIN_NIGHTS)
        assertTrue(connection.strength >= MIN_STRENGTH)
    }

    @Test fun `too few days on one side says nothing`() {
        val days = days(30) { i, date ->
            // Only three nights at or above the goal.
            DayEntry(date, strengthMinutes = 30).copy(sleepHours = if (i < 3) 9f else 6f)
        }
        assertTrue(connections(days).filterIsInstance<TrainingAfterSleep>().isEmpty())
    }

    @Test fun `the weekday with the most training stands out, with enough weeks behind it`() {
        // Three weeks, engaged with every day; only Mondays train.
        val days = days(21) { _, date ->
            DayEntry(date, waterGlasses = 5, strengthMinutes = if (date.dayOfWeek == DayOfWeek.MONDAY) 50 else 0)
        }
        val pattern = connections(days).filterIsInstance<WeekdayPattern>().single()
        assertEquals(WeekdayMetric.TrainingMinutes, pattern.metric)
        assertEquals(DayOfWeek.MONDAY, pattern.best)
        assertEquals(0.0, pattern.worstAverage, 0.001)
        assertEquals(21, pattern.days)
    }

    @Test fun `a weekday needs enough days before it says anything`() {
        // Only two Mondays: not enough to compare with the other days.
        val days = days(16) { _, date ->
            DayEntry(date, waterGlasses = 5, strengthMinutes = if (date.dayOfWeek == DayOfWeek.MONDAY) 50 else 0)
        }
        assertTrue(connections(days).filterIsInstance<WeekdayPattern>().isEmpty())
    }

    @Test fun `food shows in the energy the user logged`() {
        val days = days(40) { i, date ->
            val eaten = i % 2 == 0
            DayEntry(date, food = if (eaten) good else poor, energy = if (eaten) 4 else 2)
        }
        val connection = connections(days, limit = 4).filterIsInstance<WellbeingConnection>()
            .single { it.wellbeing == Wellbeing.Energy }
        assertEquals(WellbeingDriver.Nutrition, connection.driver)
        assertTrue(connection.difference > 0)
        assertTrue(connection.withDays >= MIN_NIGHTS && connection.withoutDays >= MIN_NIGHTS)
    }

    @Test fun `training shows in the mood the user logged`() {
        val days = days(40) { i, date ->
            val trained = i % 2 == 0
            DayEntry(date, waterGlasses = 5, mood = if (trained) 4 else 2)
                .copy(strengthMinutes = if (trained) 30 else 0)
        }
        val connection = connections(days, limit = 4).filterIsInstance<WellbeingConnection>()
            .single { it.wellbeing == Wellbeing.Mood }
        assertEquals(WellbeingDriver.Training, connection.driver)
        assertTrue(connection.difference > 0)
    }

    @Test fun `a day the user didn't engage with is unknown, not a day without training`() {
        // Nights logged, but nothing else: no training to compare, so no weekday pattern either.
        val days = days(40) { _, date -> DayEntry(date, sleepHours = 7f) }
        assertTrue(connections(days).isEmpty())
    }

    @Test fun `nothing logged finds no connections`() {
        assertTrue(connections(days(30) { _, date -> DayEntry(date) }).isEmpty())
    }

    @Test fun `the strongest comes first and the list stays within its limit`() {
        val days = days(40) { i, date ->
            val eaten = i % 2 == 0
            DayEntry(date, food = if (eaten) good else poor, energy = if (eaten) 5 else 1)
                .copy(sleepHours = (if (eaten) 9f else 6f) + wobble(i), strengthMinutes = if (eaten) 60 else 0)
        }
        val found = connections(days, limit = 2)
        assertEquals(2, found.size)
        assertTrue(found[0].ranking >= found[1].ranking)
    }
}
