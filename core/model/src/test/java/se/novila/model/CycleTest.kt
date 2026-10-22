package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleTest {
    private val start = LocalDate.of(2026, 8, 3)

    /** Five period days from each of [starts]. */
    private fun periods(vararg starts: LocalDate): Set<LocalDate> =
        starts.flatMap { day -> (0L until 5L).map { day.plusDays(it) } }.toSet()

    @Test fun `a period starts on its first day, and a forgotten day doesn't start a new one`() {
        val days = periods(start, start.plusDays(30)) - start.plusDays(2)
        assertEquals(listOf(start, start.plusDays(30)), periodStarts(days))
    }

    @Test fun `the usual length is the median of the cycles, and 28 days before there is one`() {
        assertEquals(DEFAULT_CYCLE_DAYS, cycleLength(listOf(start)))
        val starts = listOf(start, start.plusDays(30), start.plusDays(62), start.plusDays(92))
        assertEquals(30, cycleLength(starts))
        // A gap longer than any cycle (a period that was never logged) is left out.
        assertEquals(30, cycleLength(starts + start.plusDays(200)))
    }

    @Test fun `the first half of the cycle is early and the second late, from the user's own length`() {
        val days = periods(start, start.plusDays(32), start.plusDays(64))
        val last = start.plusDays(64)
        assertEquals(CyclePhase.Early, cyclePhase(days, last))
        assertEquals(CyclePhase.Early, cyclePhase(days, last.plusDays(15)))
        assertEquals(CyclePhase.Late, cyclePhase(days, last.plusDays(16)))
        // Before the next period is logged the day is still late, until the cycle says nothing any more.
        assertEquals(CyclePhase.Late, cyclePhase(days, last.plusDays(MAX_CYCLE_DAYS.toLong() - 1)))
        assertNull(cyclePhase(days, last.plusDays(MAX_CYCLE_DAYS.toLong())))
    }

    @Test fun `a day reads only the periods logged before it`() {
        val days = periods(start, start.plusDays(28))
        assertEquals(CyclePhase.Late, cyclePhase(days, start.plusDays(27)))
        assertNull(cyclePhase(days, start.minusDays(1)))
    }

    @Test fun `a day with symptoms offers a lighter session until the user chooses`() {
        val log = CycleLog().withSymptom(start, CycleSymptom.Cramps, on = true)
        assertTrue(log.lighterOffered(start))
        assertFalse(log.lighter(start))
        val lighter = log.withChoice(start, SessionChoice.Lighter)
        assertFalse(lighter.lighterOffered(start))
        assertTrue(lighter.lighter(start))
        assertFalse(log.withChoice(start, SessionChoice.AsPlanned).lighter(start))
    }

    @Test fun `without symptoms there is nothing to choose`() {
        val log = CycleLog().withPeriod(start, on = true)
        assertFalse(log.lighterOffered(start))
        assertNull(log.withChoice(start, SessionChoice.Lighter).on(start).session)
        // Taking the last symptom away takes the choice with it.
        val chosen = CycleLog()
            .withSymptom(start, CycleSymptom.Headache, on = true)
            .withChoice(start, SessionChoice.Lighter)
            .withSymptom(start, CycleSymptom.Headache, on = false)
        assertEquals(CycleLog(), chosen)
    }

    @Test fun `the log keeps only the last days`() {
        val log = CycleLog()
            .withPeriod(start, on = true)
            .withPeriod(start.plusDays(CYCLE_KEPT_DAYS), on = true)
        assertEquals(setOf(start.plusDays(CYCLE_KEPT_DAYS)), log.periodDays)
    }

    @Test fun `codec round-trips the log, and skips what it cannot read`() {
        val log = CycleLog()
            .withPeriod(start, on = true)
            .withSymptom(start, CycleSymptom.Tiredness, on = true)
            .withSymptom(start, CycleSymptom.Cramps, on = true)
            .withChoice(start, SessionChoice.Lighter)
            .withPeriod(start.plusDays(1), on = true)
        val raw = CycleLogCodec.encode(log)
        assertEquals("v1;2026-08-03:1:Cramps,Tiredness:Lighter;2026-08-04:1::", raw)
        assertEquals(log, CycleLogCodec.decode(raw))
        assertEquals(CycleLog(), CycleLogCodec.decode(null))
        assertEquals(CycleLog(), CycleLogCodec.decode("v9;2026-08-03:1::"))
        assertEquals(
            CycleLog(mapOf(start to CycleDay(symptoms = setOf(CycleSymptom.Cramps)))),
            CycleLogCodec.decode("v1;2026-08-03:0:Cramps,Unknown:;broken;2026-13-40:1::"),
        )
    }
}
