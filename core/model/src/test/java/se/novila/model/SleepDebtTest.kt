package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SleepDebtTest {
    /** A Thursday. */
    private val today = LocalDate.of(2026, 10, 22)
    private val goals = Goals(sleepHours = 8f)

    /** One night per day ending today, oldest first, [hours] by how many days ago (null leaves it unlogged). */
    private fun nights(hours: (daysAgo: Long) -> Float?): List<DayEntry> =
        (DEBT_NIGHTS - 1 downTo 0).map { ago -> DayEntry(today.minusDays(ago), sleepHours = hours(ago) ?: 0f) }

    @Test fun `nights at the goal leave no debt`() {
        val debt = sleepDebt(nights { 8f }, goals, today)
        assertEquals(0, debt.minutes)
        assertEquals(14, debt.nights)
        assertFalse(debt.notable)
    }

    /** The weight of a night [ago] days back: half after a week. */
    private fun weight(ago: Long) = Math.pow(0.5, ago / 7.0)

    @Test fun `older nights fade, half after a week`() {
        // An hour short every night.
        val debt = sleepDebt(nights { 7f }, goals, today)
        assertEquals((0L..13L).sumOf { 60 * weight(it) }, debt.minutes!!.toDouble(), 1.0)
        assertTrue(debt.notable)

        // A night a week ago counts half as much as last night.
        val lastNight = sleepDebt(nights { ago -> if (ago == 0L) 4f else 8f }, goals, today)
        val weekAgo = sleepDebt(nights { ago -> if (ago == 7L) 4f else 8f }, goals, today)
        assertEquals(240, lastNight.minutes)
        assertEquals(120, weekAgo.minutes)

        // The same short nights, but only the week before: well under half.
        val earlier = sleepDebt(nights { ago -> if (ago >= 7) 7f else 8f }, goals, today)
        assertEquals((7L..13L).sumOf { 60 * weight(it) }, earlier.minutes!!.toDouble(), 1.0)
        assertFalse(earlier.notable)
    }

    @Test fun `long nights pay back half their extra time, and the debt never goes below zero`() {
        // Two hours short on two nights, two hours extra on the two nights after them.
        val paid = sleepDebt(
            nights { ago ->
                when (ago) {
                    0L, 1L -> 10f
                    2L, 3L -> 6f
                    else -> 8f
                }
            },
            goals,
            today,
        )
        val expected = 120 * (weight(2) + weight(3)) - 0.5 * 120 * (weight(0) + weight(1))
        assertEquals(expected, paid.minutes!!.toDouble(), 1.0)
        assertTrue(paid.minutes!! > 0)
        assertEquals(0, sleepDebt(nights { 9f }, goals, today).minutes)
    }

    @Test fun `unlogged nights are unknown, and a few nights say nothing`() {
        // Five-hour nights, but only four logged.
        val few = sleepDebt(nights { ago -> if (ago < 4) 5f else null }, goals, today)
        assertNull(few.minutes)
        assertEquals(4, few.nights)
        assertFalse(few.notable)

        // Five logged, the rest unknown: only the logged nights count.
        val five = sleepDebt(nights { ago -> if (ago < 5) 7f else null }, goals, today)
        assertEquals((0L..4L).sumOf { 60 * weight(it) }, five.minutes!!.toDouble(), 1.0)
    }

    @Test fun `nights outside the two weeks don't count`() {
        val old = DayEntry(today.minusDays(DEBT_NIGHTS), sleepHours = 2f)
        assertEquals(0, sleepDebt(nights { 8f } + old, goals, today).minutes)
    }
}
