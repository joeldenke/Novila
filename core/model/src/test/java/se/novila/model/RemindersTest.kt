package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RemindersTest {
    private val today = LocalDate.of(2026, 10, 17)

    @Test fun `every reminder is off at its own time until the user turns it on`() {
        val reminders = Reminders()
        assertFalse(reminders.anyOn)
        Reminder.entries.forEach { assertEquals(ReminderSetting(on = false, time = it.defaultTime), reminders[it]) }

        val on = reminders.with(Reminder.Sleep, ReminderSetting(on = true, time = LocalTime.of(7, 0)))
        assertTrue(on.anyOn)
        assertEquals(LocalTime.of(7, 0), on[Reminder.Sleep].time)
        assertEquals(reminders[Reminder.Nutrition], on[Reminder.Nutrition])
    }

    @Test fun `nothing to remind about once it is logged`() {
        val empty = DayEntry(today)
        assertTrue(Reminder.Sleep.isNeeded(empty, trainingDay = false))
        assertFalse(Reminder.Sleep.isNeeded(empty.withSleep(7f), trainingDay = false))

        assertTrue(Reminder.Nutrition.isNeeded(empty, trainingDay = false))
        assertFalse(Reminder.Nutrition.isNeeded(empty.copy(food = FoodLog(vegetables = 1)), trainingDay = false))
        assertFalse(Reminder.Nutrition.isNeeded(empty.copy(waterGlasses = 2), trainingDay = false))
    }

    @Test fun `training only on planned days, and not once the user has trained`() {
        val empty = DayEntry(today)
        assertFalse(Reminder.Training.isNeeded(empty, trainingDay = false))
        assertTrue(Reminder.Training.isNeeded(empty, trainingDay = true))
        assertFalse(Reminder.Training.isNeeded(empty.withStrengthSession(25), trainingDay = true))
        assertFalse(
            Reminder.Training.isNeeded(empty.withCardio(CardioLog(CardioType.Walking, 30)), trainingDay = true),
        )
    }

    @Test fun `the next reminder is later today, or tomorrow once the time has passed`() {
        val morning = LocalDateTime.of(today, LocalTime.of(7, 0))
        assertEquals(today.atTime(8, 30), nextReminderAt(morning, LocalTime.of(8, 30)))
        assertEquals(today.plusDays(1).atTime(6, 0), nextReminderAt(morning, LocalTime.of(6, 0)))
        // At the minute itself it has just been sent, so the next is tomorrow.
        assertEquals(today.plusDays(1).atTime(7, 0), nextReminderAt(morning, LocalTime.of(7, 0)))
    }
}
