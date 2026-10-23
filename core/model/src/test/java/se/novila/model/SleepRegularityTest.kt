package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class SleepRegularityTest {
    /** A Thursday. */
    private val today = LocalDate.of(2026, 10, 22)

    /** [count] nights ending today, oldest first, each from [times] by its date (null leaves the night unlogged). */
    private fun nights(count: Int, times: (LocalDate) -> Pair<LocalTime, LocalTime>?): List<DayEntry> =
        (count - 1 downTo 0).map { today.minusDays(it.toLong()) }.map { date ->
            val night = times(date)
            if (night == null) DayEntry(date) else DayEntry(date).withSleepTimes(night.first, night.second)
        }

    private val steady = LocalTime.of(23, 0) to LocalTime.of(7, 0)

    @Test fun `the same night every night is perfectly regular`() {
        val regularity = sleepRegularity(nights(14) { steady }, today)
        assertEquals(100, regularity.index)
        assertEquals(13, regularity.pairs)
        assertEquals(RegularityLevel.Steady, regularity.level)
        assertEquals(0, regularity.socialJetlagMinutes)
    }

    @Test fun `nights that shift a lot from one to the next are irregular`() {
        // Every other night four hours later, to bed and up.
        val regularity = sleepRegularity(
            nights(14) { date ->
                if (date.dayOfMonth % 2 == 0) steady else LocalTime.of(3, 0) to LocalTime.of(11, 0)
            },
            today,
        )
        // Four hours of the day differ in both directions: 1 - 8/24 agree.
        assertEquals((200 * (1 - 8.0 / 24) - 100).toInt(), regularity.index)
        assertEquals(RegularityLevel.Irregular, regularity.level)
    }

    @Test fun `bedtimes either side of midnight are close, not a day apart`() {
        val regularity = sleepRegularity(
            nights(14) { date ->
                val bedtime = if (date.dayOfMonth % 2 == 0) LocalTime.of(23, 45) else LocalTime.of(0, 15)
                bedtime to LocalTime.of(7, 0)
            },
            today,
        )
        // Half an hour of each day differs.
        assertEquals(96, regularity.index)
        assertEquals(RegularityLevel.Steady, regularity.level)
    }

    @Test fun `too few nights in a row say nothing, and unlogged nights are unknown`() {
        // Every other night logged: no two in a row.
        val regularity = sleepRegularity(nights(28) { date -> steady.takeIf { date.dayOfMonth % 2 == 0 } }, today)
        assertNull(regularity.index)
        assertEquals(0, regularity.pairs)
        assertNull(regularity.level)
    }

    @Test fun `sleeping in at weekends is social jetlag`() {
        val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        val regularity = sleepRegularity(
            nights(28) { date ->
                if (date.dayOfWeek in weekend) LocalTime.of(1, 0) to LocalTime.of(10, 0) else steady
            },
            today,
        )
        // The middle of the night moves from 03:00 to 05:30.
        assertEquals(150, regularity.socialJetlagMinutes)
        assertTrue(regularity.notableJetlag)
        assertEquals(8, regularity.weekendNights)
        assertEquals(20, regularity.weekdayNights)
    }

    @Test fun `a single weekend night gives no social jetlag`() {
        // Sunday to Thursday: one weekend morning.
        val regularity = sleepRegularity(nights(5) { steady }, today)
        assertNull(regularity.socialJetlagMinutes)
        assertFalse(regularity.notableJetlag)
    }

    @Test fun `the body clock is the middle of the sleep on free days`() {
        // Weekdays 23:00-07:00, free days 00:30-08:30: the same length, so no catch-up sleep.
        val days = nights(28) { date ->
            if (date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                LocalTime.of(0, 30) to LocalTime.of(8, 30)
            } else {
                steady
            }
        }
        val clock = bodyClock(days, today)!!
        assertEquals(LocalTime.of(4, 30), clock.midSleep)
        assertEquals(8, clock.freeNights)
        assertEquals(20, clock.workNights)
        // Eight hours around 04:30 start at 00:30; seven and a half at 00:45.
        assertEquals(LocalTime.of(0, 30), clock.bedtimeFor(8f))
        assertEquals(LocalTime.of(0, 45), clock.bedtimeFor(7.5f))
    }

    @Test fun `catch-up sleep on free days is taken out of the body clock`() {
        // Weekdays 00:00-06:00 (six hours), free days 00:00-10:00 (ten hours, middle 05:00).
        val days = nights(28) { date ->
            if (date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                LocalTime.MIDNIGHT to LocalTime.of(10, 0)
            } else {
                LocalTime.MIDNIGHT to LocalTime.of(6, 0)
            }
        }
        // The week's average night is (5 * 6 + 2 * 10) / 7 hours; half of the extra on free days is catch-up.
        val week = (5 * 360 + 2 * 600) / 7.0
        val expected = (5 * 60 - (600 - week) / 2).toInt()
        val clock = bodyClock(days, today)!!
        assertEquals(LocalTime.of(expected / 60, expected % 60), clock.midSleep)
    }

    @Test fun `the body clock needs free and work nights with times`() {
        // Only weekdays logged.
        val weekdays = nights(28) { date ->
            if (date.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) null else steady
        }
        assertNull(bodyClock(weekdays, today))
        assertNull(bodyClock(emptyList(), today))
    }
}
