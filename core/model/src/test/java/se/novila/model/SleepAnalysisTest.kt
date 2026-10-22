package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SleepAnalysisTest {
    private val today = LocalDate.of(2026, 10, 17)
    private val good = FoodLog(vegetables = 5, meals = 3, protein = 3, wholeGrain = 3)
    private val poor = FoodLog(vegetables = 1, meals = 2, protein = 1, wholeGrain = 0, treats = 3)

    /** [count] days ending today, oldest first, each made by [day] from its index (0 is the oldest). */
    private fun days(count: Int, day: (Int, LocalDate) -> DayEntry): List<DayEntry> =
        (0 until count).map { i -> day(i, today.minusDays((count - 1 - i).toLong())) }

    /** A little variation, so the groups aren't flat: -0.25, 0, +0.25 h in turn. */
    private fun wobble(i: Int) = ((i % 3) - 1) * 0.25f

    @Test fun `nights after eating well are longer, and the link says by how much and over how many nights`() {
        // Every other day eaten well; the night after is an hour longer.
        val days = days(40) { i, date ->
            val ateWellBefore = (i - 1) % 2 == 0
            DayEntry(date, drinks = glassesOfWater(8), food = if (i % 2 == 0) good else poor)
                .copy(sleepHours = (if (ateWellBefore) 8f else 7f) + wobble(i))
        }
        val analysis = analyzeSleep(days)
        val link = analysis.links.first { it.driver == SleepDriver.AteWell && it.outcome == SleepOutcome.Hours }
        assertEquals(1.0, link.difference, 0.15)
        assertTrue(link.withNights >= MIN_NIGHTS && link.withoutNights >= MIN_NIGHTS)
        assertTrue(link.strength >= 2.0)
    }

    @Test fun `small or noisy differences are not links`() {
        // Ten minutes longer after eating well, and noise on top: nothing worth telling.
        val days = days(40) { i, date ->
            val ateWellBefore = (i - 1) % 2 == 0
            DayEntry(date, drinks = glassesOfWater(8), food = if (i % 2 == 0) good else poor)
                .copy(sleepHours = 7f + (if (ateWellBefore) 1f / 6 else 0f) + wobble(i) * 2)
        }
        assertTrue(analyzeSleep(days).links.none { it.driver == SleepDriver.AteWell })
    }

    @Test fun `too few nights on one side say nothing`() {
        val days = days(20) { i, date ->
            // Only two days eaten well.
            DayEntry(date, drinks = glassesOfWater(8), food = if (i == 3 || i == 9) good else poor)
                .copy(sleepHours = if (i == 4 || i == 10) 9f else 7f)
        }
        assertTrue(analyzeSleep(days).links.none { it.driver == SleepDriver.AteWell })
    }

    @Test fun `days without logs are unknown, not days without training`() {
        // Trained on logged days and slept long after; the unlogged days in between are left out.
        val days = days(30) { i, date ->
            when {
                i % 3 == 0 -> DayEntry(date, strengthMinutes = 30, sleepHours = 7f)
                i % 3 == 1 -> DayEntry(date, sleepHours = 8f + wobble(i))
                else -> DayEntry(date, sleepHours = 7f + wobble(i))
            }
        }
        // Every logged day had training, so there is nothing to compare it with.
        assertTrue(analyzeSleep(days).links.none { it.driver == SleepDriver.Trained })
    }

    @Test fun `factors count only on nights with details, and late caffeine shows as worse sleep`() {
        val days = days(30) { i, date ->
            val caffeine = i % 2 == 0
            DayEntry(date)
                .copy(sleepHours = (if (caffeine) 6.5f else 7.5f) + wobble(i))
                .withSleepQuality(if (caffeine) 2 + i % 2 else 4)
                .withSleepFactor(SleepFactor.LateCaffeine, caffeine)
                .withSleepFactor(SleepFactor.Screens, i % 5 == 0)
        }
        val links = analyzeSleep(days, limit = 10).links
        // One link per driver: the stronger of hours and quality.
        val caffeine = links.single { it.driver == SleepDriver.LateCaffeine }
        assertTrue(caffeine.difference < 0)
    }

    @Test fun `nights marked with a period are tested like the other factors`() {
        // A period on six nights in a row each month, with half an hour less sleep and a worse quality on them.
        val days = days(60) { i, date ->
            val period = i % 28 < 6
            DayEntry(date)
                .copy(sleepHours = (if (period) 6.5f else 7.5f) + wobble(i))
                .withSleepQuality(if (period) 2 else 4)
                .withSleepFactor(SleepFactor.Period, period)
        }
        val period = analyzeSleep(days, limit = 10).links.single { it.driver == SleepDriver.Period }
        assertTrue(period.difference < 0)
        assertTrue(period.withNights >= MIN_NIGHTS)
    }

    @Test fun `a period says nothing about nights without details`() {
        // Only the period nights have details, so there are no nights without one to compare with.
        val days = days(60) { i, date ->
            val period = i % 28 < 6
            DayEntry(date, sleepHours = (if (period) 6f else 8f) + wobble(i))
                .withSleepFactor(SleepFactor.Period, period)
        }
        assertTrue(analyzeSleep(days, limit = 10).links.none { it.driver == SleepDriver.Period })
    }

    @Test fun `coffee logged late the day before counts as late caffeine without the factor`() {
        // Every other day a coffee at four in the afternoon, the others at eight in the morning; the night after a
        // late one is an hour shorter. No night has details, so only the drinks tell.
        val days = days(30) { i, date ->
            val late = i % 2 == 0
            DayEntry(date)
                .withDrink(Drink(DrinkKind.Coffee, 150, LocalTime.of(if (late) 16 else 8, 0)))
                .copy(sleepHours = (if (i % 2 == 1) 6.5f else 7.5f) + wobble(i))
        }
        val caffeine = analyzeSleep(days, limit = 10).links.single { it.driver == SleepDriver.LateCaffeine }
        assertTrue(caffeine.difference < 0)
    }

    @Test fun `a better week comes with what changed in it, and is confirmed by the longer history`() {
        // Eight weeks: eating well gives an hour more sleep. The last week has five good days, the one before one.
        val days = days(56) { i, date ->
            val week = i / 7
            val ateWell = when (week) {
                6 -> i % 7 == 0
                7 -> i % 7 < 5
                else -> i % 2 == 0
            }
            DayEntry(date, drinks = glassesOfWater(8), food = if (ateWell) good else poor)
        }.let { plain ->
            // The night after a good day is an hour longer.
            plain.mapIndexed { i, day ->
                val before = plain.getOrNull(i - 1)?.food == good
                day.copy(sleepHours = (if (before) 8f else 7f) + wobble(i))
            }
        }
        val change = checkNotNull(analyzeSleep(days).change)
        assertEquals(SleepOutcome.Hours, change.outcome)
        assertTrue(change.change > 0.3)
        val ateWell = change.drivers.single { it.driver == SleepDriver.AteWell }
        assertTrue(ateWell.now - ateWell.before >= 2)
        assertTrue(change.confirmed)
    }

    @Test fun `a changed week without anything that explains it is left to the history chart`() {
        val days = days(28) { i, date -> DayEntry(date, sleepHours = if (i >= 21) 8.5f else 7f) }
        assertNull(analyzeSleep(days).change)
    }

    @Test fun `bedtimes that vary around midnight are measured across it`() {
        val steady = days(14) { i, date ->
            val bed = if (i % 2 == 0) LocalTime.of(23, 40) else LocalTime.of(0, 10)
            DayEntry(date).withSleepTimes(bed, LocalTime.of(7, 0))
        }
        // 23:40 and 00:10 are half an hour apart, not 23 hours.
        assertNull(analyzeSleep(steady).bedtimes)

        val irregular = days(14) { i, date ->
            val bed = if (i % 2 == 0) LocalTime.of(22, 0) else LocalTime.of(1, 0)
            DayEntry(date).withSleepTimes(bed, LocalTime.of(7, 0))
        }
        val spread = checkNotNull(analyzeSleep(irregular).bedtimes)
        assertTrue(spread.spreadMinutes in 80..100)
    }

    @Test fun `nothing logged is an empty analysis`() {
        val analysis = analyzeSleep(days(30) { _, date -> DayEntry(date) })
        assertTrue(analysis.isEmpty)
        assertEquals(0, analysis.nightsLogged)
        assertEquals(29, analyzeSleep(days(30) { _, date -> DayEntry(date, sleepHours = 7f) }).nightsLogged)
    }

    @Test fun `welch t grows with the difference and the nights behind it`() {
        val a = listOf(8.0, 8.25, 7.75, 8.0)
        val b = listOf(7.0, 7.25, 6.75, 7.0)
        assertTrue(welchT(a, b) > 5)
        assertTrue(welchT(a + a, b + b) > welchT(a, b))
        assertEquals(0.0, welchT(a, a), 1e-9)
    }
}
