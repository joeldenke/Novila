package se.novila

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DayEntryTest {
    private val date = LocalDate.of(2026, 10, 14)
    private val sv = Locale.forLanguageTag("sv-SE")

    @Test fun `codec round-trips an entry`() {
        val entry = DayEntry(date, waterGlasses = 5, sleepHours = 7.5f, mood = 4)
        assertEquals(entry, DayEntryCodec.decode(date, DayEntryCodec.encode(entry)))
    }

    @Test fun `missing or corrupt data decodes to an empty day`() {
        assertEquals(DayEntry(date), DayEntryCodec.decode(date, null))
        assertEquals(DayEntry(date), DayEntryCodec.decode(date, "skräp"))
        assertEquals(DayEntry(date), DayEntryCodec.decode(date, "v9;1;2;3"))
    }

    @Test fun `water never goes below zero`() {
        assertEquals(0, DayEntry(date).withWater(-1).waterGlasses)
        assertEquals(1, DayEntry(date).withWater(+1).waterGlasses)
    }

    @Test fun `water progress is capped at the goal`() {
        assertEquals(0.5f, DayEntry(date, waterGlasses = WATER_GOAL / 2).waterProgress, 0.0001f)
        assertEquals(1f, DayEntry(date, waterGlasses = WATER_GOAL * 2).waterProgress, 0.0001f)
    }

    @Test fun `sleep rounds to half hours and stays in range`() {
        assertEquals(7.5f, DayEntry(date).withSleep(7.4f).sleepHours, 0f)
        assertEquals(7f, DayEntry(date).withSleep(7.2f).sleepHours, 0f)
        assertEquals(0f, DayEntry(date).withSleep(-3f).sleepHours, 0f)
        assertEquals(MAX_SLEEP_HOURS, DayEntry(date).withSleep(20f).sleepHours, 0f)
    }

    @Test fun `mood is clamped to 0 to 5`() {
        assertEquals(5, DayEntry(date).withMood(9).mood)
        assertEquals(0, DayEntry(date).withMood(-1).mood)
    }

    @Test fun `hours are formatted per locale`() {
        assertNull(formatHours(0f, sv))
        assertEquals("8", formatHours(8f, sv))
        assertEquals("7,5", formatHours(7.5f, sv))
        assertEquals("7.5", formatHours(7.5f, Locale.US))
    }
}
