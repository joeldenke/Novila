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
        val food = FoodLog(vegetables = 4, meals = 3, protein = 2, wholeGrain = 1, treats = 2)
        val entry = DayEntry(date, waterGlasses = 5, sleepHours = 7.5f, mood = 4, food = food, strengthMinutes = 32)
        assertEquals(entry, DayEntryCodec.decode(date, DayEntryCodec.encode(entry)))
    }

    @Test fun `v2 data from before session mode is migrated`() {
        assertEquals(
            DayEntry(date, waterGlasses = 5, sleepHours = 7.5f, mood = 4, food = FoodLog(4, 3, 2, 1, 2)),
            DayEntryCodec.decode(date, "v2;5;7.5;4;4;3;2;1;2"),
        )
    }

    @Test fun `strength sessions add up within a day`() {
        val day = DayEntry(date).withStrengthSession(30).withStrengthSession(0)
        assertEquals(31, day.strengthMinutes)
        assertEquals(MAX_STRENGTH_MINUTES, day.withStrengthSession(10_000).strengthMinutes)
    }

    @Test fun `v1 data from before food logging is migrated`() {
        assertEquals(
            DayEntry(date, waterGlasses = 5, sleepHours = 7.5f, mood = 4),
            DayEntryCodec.decode(date, "v1;5;7.5;4"),
        )
    }

    @Test fun `missing or corrupt data decodes to an empty day`() {
        assertEquals(DayEntry(date), DayEntryCodec.decode(date, null))
        assertEquals(DayEntry(date), DayEntryCodec.decode(date, "skräp"))
        assertEquals(DayEntry(date), DayEntryCodec.decode(date, "v9;1;2;3"))
        assertEquals(DayEntry(date), DayEntryCodec.decode(date, "v2;1;2;3"))
        assertEquals(DayEntry(date), DayEntryCodec.decode(date, "v3;1;2;3;4;5;6;7;8"))
    }

    @Test fun `portions never go below zero`() {
        assertEquals(0, DayEntry(date).withPortions(FoodGroup.Water, -1).waterGlasses)
        assertEquals(1, DayEntry(date).withPortions(FoodGroup.Water, +1).waterGlasses)
        assertEquals(0, DayEntry(date).withPortions(FoodGroup.Protein, -1).food.protein)
        assertEquals(0, DayEntry(date).withTreats(-1).food.treats)
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
