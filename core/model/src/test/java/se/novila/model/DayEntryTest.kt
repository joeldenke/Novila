package se.novila.model

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

    @Test fun `codec round-trips cardio and feedback`() {
        val entry = DayEntry(date, strengthMinutes = 25)
            .withCardio(CardioLog(CardioType.Walking, 30))
            .withCardio(CardioLog(CardioType.Running, 20))
            .withFeedback(effort = 7, pain = 0, phase = 2)
        assertEquals(entry, DayEntryCodec.decode(date, DayEntryCodec.encode(entry)))
        assertEquals("v4;0;0.0;0;0;0;0;0;0;25;Walking:30,Running:20;7;0;2", DayEntryCodec.encode(entry))
        val empty = DayEntry(date)
        assertEquals(empty, DayEntryCodec.decode(date, DayEntryCodec.encode(empty)))
    }

    @Test fun `v3 data from before cardio logging is migrated`() {
        assertEquals(
            DayEntry(date, waterGlasses = 5, sleepHours = 7.5f, mood = 4, food = FoodLog(4, 3, 2, 1, 2))
                .withStrengthSession(32),
            DayEntryCodec.decode(date, "v3;5;7.5;4;4;3;2;1;2;32"),
        )
    }

    @Test fun `unknown cardio types are skipped`() {
        val day = DayEntryCodec.decode(date, "v4;0;0;0;0;0;0;0;0;0;Rowing:20,Walking:15,Walking:x;0;;0")
        assertEquals(listOf(CardioLog(CardioType.Walking, 15)), day.cardio)
        assertNull(day.pain)
    }

    @Test fun `feedback keeps the hardest session of the day`() {
        val day = DayEntry(date).withFeedback(effort = 8, pain = 2).withFeedback(effort = 5, pain = 4)
        assertEquals(8, day.effort)
        assertEquals(4, day.pain)
        assertEquals(0, day.rehabPhase)
        val unanswered = DayEntry(date).withFeedback(effort = null, pain = null)
        assertEquals(0, unanswered.effort)
        assertNull(unanswered.pain)
        assertEquals(MAX_EFFORT, DayEntry(date).withFeedback(effort = 15, pain = -2).effort)
        assertEquals(0, DayEntry(date).withFeedback(effort = 15, pain = -2).pain)
    }

    @Test fun `cardio adds up within a day and can be taken back`() {
        val day = DayEntry(date)
            .withCardio(CardioLog(CardioType.Walking, 30))
            .withCardio(CardioLog(CardioType.Cycling, 0))
        assertEquals(31, day.cardioMinutes)
        assertEquals(30, day.withoutLastCardio().cardioMinutes)
        assertEquals(MAX_CARDIO_MINUTES, day.withCardio(CardioLog(CardioType.Running, 10_000)).cardioMinutes)
        val full = day.withCardio(CardioLog(CardioType.Running, 10_000))
        assertEquals(full, full.withCardio(CardioLog(CardioType.Running, 5)))
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
        assertEquals(DayEntry(date), DayEntryCodec.decode(date, "v4;1;2;3;4;5;6;7;8;9"))
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
