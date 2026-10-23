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

    @Test fun `the last week counts in full and the week before it half`() {
        // An hour short every night: seven hours this week and three and a half the week before.
        val debt = sleepDebt(nights { 7f }, goals, today)
        assertEquals((7 + 3.5) * 60, debt.minutes!!.toDouble(), 0.0)
        assertTrue(debt.notable)

        // The same short nights, but only the week before: half as much.
        val earlier = sleepDebt(nights { ago -> if (ago >= 7) 7f else 8f }, goals, today)
        assertEquals(210, earlier.minutes)
        assertFalse(earlier.notable)
    }

    @Test fun `long nights pay back, but the debt never goes below zero`() {
        // Two hours short on two nights, two hours extra on two others this week.
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
        assertEquals(0, paid.minutes)
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
        assertEquals(5 * 60, five.minutes)
    }

    @Test fun `nights outside the two weeks don't count`() {
        val old = DayEntry(today.minusDays(DEBT_NIGHTS), sleepHours = 2f)
        assertEquals(0, sleepDebt(nights { 8f } + old, goals, today).minutes)
    }
}
