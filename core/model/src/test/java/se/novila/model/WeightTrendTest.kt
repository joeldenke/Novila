package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeightTrendTest {
    private val today = LocalDate.of(2026, 10, 22)

    private fun weighIns(vararg kgByDaysAgo: Pair<Long, Int>) =
        kgByDaysAgo.map { (ago, kg) -> MeasurementEntry(today.minusDays(ago), BodyMeasurements(weightKg = kg)) }

    @Test fun `a single weigh-in has no trend, and days without a weight are skipped`() {
        assertNull(weightTrend(weighIns(0L to 73)))
        val waistOnly = MeasurementEntry(today.minusDays(3), BodyMeasurements(waistCm = 80))
        assertNull(weightTrend(weighIns(0L to 73) + waistOnly))
    }

    @Test fun `each weigh-in moves the trend a tenth of the way per day since the last one`() {
        // One day apart: a tenth of the way from 70 to 80.
        val oneDay = weightTrend(weighIns(1L to 70, 0L to 80))!!
        assertEquals(71.0, oneDay.latestKg, 1e-9)
        // Seven days apart: 1 - 0.9^7 of the way.
        val week = weightTrend(weighIns(7L to 70, 0L to 80))!!
        assertEquals(70 + 10 * (1 - Math.pow(0.9, 7.0)), week.latestKg, 1e-9)
        assertNull(week.weeklyChangeKg)
        assertNull(week.direction)
    }

    @Test fun `a day's swing barely moves the trend`() {
        // Daily at 73 for four weeks, then one heavy day after a salty dinner.
        val days = (28L downTo 1L).map { it to 73 } + (0L to 75)
        val trend = weightTrend(weighIns(*days.toTypedArray()))!!
        assertEquals(73.2, trend.latestKg, 1e-9)
        assertEquals(WeightDirection.Steady, trend.direction)
    }

    @Test fun `the weekly change looks back two to four weeks, in neutral words`() {
        // A kilo less every week, weighed weekly for five weeks.
        val falling = weightTrend(weighIns(28L to 78, 21L to 77, 14L to 76, 7L to 75, 0L to 74))!!
        assertEquals(4, falling.weeks)
        assertEquals(WeightDirection.Lower, falling.direction)
        // The line through the weigh-ins, not the lagging average: exactly a kilo a week.
        assertEquals(-1.0, falling.weeklyChangeKg!!, 1e-9)

        val rising = weightTrend(weighIns(14L to 70, 7L to 71, 0L to 72))!!
        assertEquals(2, rising.weeks)
        assertEquals(WeightDirection.Higher, rising.direction)

        // A change that has just begun: the line sees it in full, while the trend still lags behind.
        val days = (21L downTo 7L).map { it to 80 } + (6L downTo 0L).map { it to 79 }
        val begun = weightTrend(weighIns(*days.toTypedArray()))!!
        assertTrue(begun.weeklyChangeKg!! < -0.3)

        // Weigh-ins older than four weeks don't count for the change.
        val old = weightTrend(weighIns(60L to 90, 0L to 74))!!
        assertNull(old.weeklyChangeKg)
    }
}
