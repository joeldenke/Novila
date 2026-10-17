package se.novila.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import se.novila.model.DayEntry
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SleepTest {
    private val monday = LocalDate.of(2026, 10, 12)
    private val tuesday = monday.plusDays(1)

    private fun session(start: String, end: String, awake: Int = 0) =
        SleepSession(LocalDateTime.parse(start), LocalDateTime.parse(end), awake)

    @Test fun `a night counts on the day the user woke up`() {
        val nights = nightsOf(listOf(session("2026-10-12T23:10:30", "2026-10-13T06:50:10", awake = 20)))
        assertEquals(listOf(Night(tuesday, LocalTime.of(23, 10), LocalTime.of(6, 50), asleepMinutes = 439)), nights)
    }

    @Test fun `naps are not the night`() {
        val nights = nightsOf(
            listOf(
                session("2026-10-12T23:00", "2026-10-13T06:00"),
                session("2026-10-13T14:00", "2026-10-13T14:40"),
                session("2026-10-14T13:00", "2026-10-14T14:30"),
            ),
        )
        assertEquals(listOf(tuesday), nights.map { it.date })
        assertEquals(LocalTime.of(23, 0), nights.single().bedtime)
    }

    @Test fun `the longest session that ended on a day is its night`() {
        val nights = nightsOf(
            listOf(
                session("2026-10-12T05:00", "2026-10-12T08:30"),
                session("2026-10-11T22:00", "2026-10-12T04:00"),
            ),
        )
        assertEquals(LocalTime.of(22, 0), nights.single().bedtime)
        assertEquals(monday, nights.single().date)
    }

    @Test fun `awake time never makes the sleep negative`() {
        val night = nightsOf(listOf(session("2026-10-12T23:00", "2026-10-13T03:00", awake = 500))).single()
        assertEquals(0, night.asleepMinutes)
    }

    @Test fun `a recorded night fills in a day without sleep`() {
        val night = Night(tuesday, LocalTime.of(23, 0), LocalTime.of(7, 0), asleepMinutes = 450)
        val day = DayEntry(tuesday, sleepQuality = 4).withNight(night)
        assertEquals(7.5f, day.sleepHours)
        assertEquals(LocalTime.of(23, 0), day.bedtime)
        assertEquals(LocalTime.of(7, 0), day.wakeTime)
        assertEquals(4, day.sleepQuality)
    }

    @Test fun `the user's own log wins over a recorded night`() {
        val night = Night(tuesday, LocalTime.of(23, 0), LocalTime.of(7, 0), asleepMinutes = 450)
        val logged = DayEntry(tuesday).withSleep(6f)
        assertSame(logged, logged.withNight(night))
        val timed = DayEntry(tuesday).withSleepTimes(LocalTime.of(22, 0), LocalTime.of(22, 0))
        assertSame(timed, timed.withNight(night))
    }

    @Test fun `nights by day only when connected`() {
        val night = Night(tuesday, LocalTime.of(23, 0), LocalTime.of(7, 0), asleepMinutes = 450)
        assertEquals(mapOf(tuesday to night), SleepState.Connected(listOf(night)).nightsByDate)
        assertEquals(emptyMap<LocalDate, Night>(), SleepState.NeedsPermission().nightsByDate)
    }
}
