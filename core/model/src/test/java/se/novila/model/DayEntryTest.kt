package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
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
        assertEquals("v6;0;0.0;0;0;0;0;0;0;25;Walking:30,Running:20;7;0;2;;;0;;0;0;", DayEntryCodec.encode(entry))
        val empty = DayEntry(date)
        assertEquals(empty, DayEntryCodec.decode(date, DayEntryCodec.encode(empty)))
    }

    @Test fun `codec round-trips energy, stress and the diary line`() {
        val entry = DayEntry(date).withEnergy(4).withStress(2).withDiary("Long walk; legs heavy")
        assertEquals("Long walk, legs heavy", entry.diary)
        assertEquals(entry, DayEntryCodec.decode(date, DayEntryCodec.encode(entry)))
        assertEquals("v6;0;0.0;0;0;0;0;0;0;0;;0;;0;;;0;;4;2;Long walk, legs heavy", DayEntryCodec.encode(entry))
    }

    @Test fun `codec round-trips the night with its times, quality and factors`() {
        val entry = DayEntry(date)
            .withSleepTimes(LocalTime.of(23, 15), LocalTime.of(6, 45))
            .withSleepQuality(4)
            .withSleepFactor(SleepFactor.Screens, on = true)
            .withSleepFactor(SleepFactor.LateCaffeine, on = true)
        assertEquals(entry, DayEntryCodec.decode(date, DayEntryCodec.encode(entry)))
        assertEquals(
            "v6;0;7.5;0;0;0;0;0;0;0;;0;;0;1395;405;4;LateCaffeine,Screens;0;0;",
            DayEntryCodec.encode(entry),
        )
    }

    @Test fun `v4 data from before bedtimes is migrated with its hours`() {
        val day = DayEntryCodec.decode(date, "v4;5;7.5;4;4;3;2;1;2;25;Walking:30;7;0;2")
        assertEquals(7.5f, day.sleepHours)
        assertNull(day.bedtime)
        assertEquals(0, day.sleepQuality)
        assertEquals(emptySet<SleepFactor>(), day.sleepFactors)
        assertEquals(listOf(CardioLog(CardioType.Walking, 30)), day.cardio)
    }

    @Test fun `bed and wake times give the hours, across midnight`() {
        val night = DayEntry(date).withSleepTimes(LocalTime.of(22, 50), LocalTime.of(6, 20))
        assertEquals(7.5f, night.sleepHours)
        assertEquals(0f, DayEntry(date).withSleepTimes(LocalTime.of(7, 0), LocalTime.of(7, 0)).sleepHours)
        assertEquals(MAX_SLEEP_HOURS, DayEntry(date).withSleepTimes(LocalTime.of(18, 0), LocalTime.of(9, 0)).sleepHours)
        // Hours set by hand no longer match the times.
        assertNull(night.withSleep(8f).bedtime)
    }

    @Test fun `unknown factors and out of range values are skipped`() {
        val day = DayEntryCodec.decode(date, "v6;0;0;0;0;0;0;0;0;0;;0;;0;9999;x;9;Alcohol,Snoring;9;0;")
        assertNull(day.bedtime)
        assertNull(day.wakeTime)
        assertEquals(MAX_SLEEP_QUALITY, day.sleepQuality)
        assertEquals(5, day.energy)
        assertEquals(0, day.stress)
        assertEquals(setOf(SleepFactor.Alcohol), day.sleepFactors)
    }

    @Test fun `v5 data from before energy, stress and the diary line is migrated`() {
        val day = DayEntryCodec.decode(date, "v5;5;7.5;4;4;3;2;1;2;25;Walking:30;7;0;2;1395;405;4;LateCaffeine")
        assertEquals(4, day.mood)
        assertEquals(0, day.energy)
        assertEquals(0, day.stress)
        assertEquals("", day.diary)
        assertEquals(7.5f, day.sleepHours)
        assertEquals(listOf(CardioLog(CardioType.Walking, 30)), day.cardio)
        assertEquals(LocalTime.of(23, 15), day.bedtime)
        assertEquals(4, day.sleepQuality)
        assertEquals(setOf(SleepFactor.LateCaffeine), day.sleepFactors)
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
        assertEquals(DayEntry(date), DayEntryCodec.decode(date, "v6;1;2;3"))
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

    @Test fun `mood, energy and stress are clamped to 0 to 5`() {
        val day = DayEntry(date).withMood(9).withEnergy(9).withStress(-1)
        assertEquals(5, day.mood)
        assertEquals(5, day.energy)
        assertEquals(0, day.stress)
        assertEquals(0, DayEntry(date).withMood(-1).mood)
    }

    @Test fun `the diary line is one short line`() {
        assertEquals("Long walk, legs heavy", DayEntry(date).withDiary("  Long walk; legs\nheavy  ").diary)
        assertEquals("", DayEntry(date).withDiary("\n").diary)
        assertEquals(MAX_DIARY_LENGTH, DayEntry(date).withDiary("x".repeat(500)).diary.length)
    }

    @Test fun `hours are formatted per locale`() {
        assertNull(formatHours(0f, sv))
        assertEquals("8", formatHours(8f, sv))
        assertEquals("7,5", formatHours(7.5f, sv))
        assertEquals("7.5", formatHours(7.5f, Locale.US))
    }

    @Test fun `hours round to the nearest half hour`() {
        assertEquals(0f, 0f.roundToHalfHour(), 0f)
        assertEquals(7f, 7.2f.roundToHalfHour(), 0f)
        assertEquals(7.5f, 7.4f.roundToHalfHour(), 0f)
        assertEquals(7.5f, 7.25f.roundToHalfHour(), 0f)
        assertEquals(8f, 7.75f.roundToHalfHour(), 0f)
    }
}
