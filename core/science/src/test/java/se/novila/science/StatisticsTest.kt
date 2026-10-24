package se.novila.science

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsTest {
    @Test fun `benjamini-hochberg adjusts in rank order and keeps the input order`() {
        assertEquals(listOf(0.02, 0.04, 0.04, 0.02), benjaminiHochberg(listOf(0.01, 0.04, 0.03, 0.005)))
        assertEquals(listOf(1.0), benjaminiHochberg(listOf(1.0)))
        assertEquals(emptyList<Double>(), benjaminiHochberg(emptyList()))
    }

    @Test fun `one small p among many is no longer a finding`() {
        val q = benjaminiHochberg(listOf(0.04) + List(19) { 0.5 })
        assertTrue(q[0] > 0.1)
    }

    @Test fun `kruskal-wallis matches the hand computation`() {
        val test = kruskalWallis(listOf(listOf(1.0, 2.0, 3.0), listOf(4.0, 5.0, 6.0)))!!
        assertEquals(12.0 / 42 * (36.0 / 3 + 225.0 / 3) - 21, test.h, 1e-12)
        assertEquals(1, test.df)
        assertEquals(0.0495, test.p, 1e-3)
    }

    @Test fun `kruskal-wallis corrects for ties and needs some variation`() {
        val tied = kruskalWallis(listOf(listOf(1.0, 1.0, 2.0), listOf(2.0, 3.0, 3.0)))!!
        val untied = kruskalWallis(listOf(listOf(1.0, 1.1, 2.0), listOf(2.1, 3.0, 3.1)))!!
        assertTrue(tied.h < untied.h + 1e-9)
        assertNull(kruskalWallis(listOf(listOf(1.0, 1.0), listOf(1.0, 1.0))))
        assertNull(kruskalWallis(listOf(listOf(1.0, 2.0))))
    }

    @Test fun `similar groups give a large p`() {
        val test = kruskalWallis(listOf(listOf(1.0, 4.0, 5.0), listOf(2.0, 3.0, 6.0), listOf(1.5, 3.5, 5.5)))!!
        assertTrue(test.p > 0.5)
    }

    @Test fun `least squares finds the line and its standard error`() {
        val exact = linearTrend(listOf(0.0, 1.0, 2.0, 3.0), listOf(1.0, 3.0, 5.0, 7.0))!!
        assertEquals(2.0, exact.slope, 1e-12)
        assertEquals(1.0, exact.intercept, 1e-12)
        assertEquals(0.0, exact.standardError, 1e-12)
        val noisy = linearTrend(listOf(0.0, 1.0, 2.0, 3.0), listOf(1.0, 2.0, 2.0, 3.0))!!
        assertEquals(0.6, noisy.slope, 1e-12)
        assertTrue(noisy.standardError > 0)
        assertNull(linearTrend(listOf(1.0, 1.0), listOf(1.0, 2.0)))
        assertNull(linearTrend(listOf(1.0), listOf(1.0)))
    }

    @Test fun `least squares with several columns`() {
        // y = 1 + 2a + 3b exactly.
        val rows = listOf(
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(1.0, 1.0, 0.0),
            doubleArrayOf(1.0, 0.0, 1.0),
            doubleArrayOf(1.0, 1.0, 1.0),
            doubleArrayOf(1.0, 2.0, 1.0),
        )
        val fit = leastSquares(rows, rows.map { 1 + 2 * it[1] + 3 * it[2] })!!
        assertEquals(1.0, fit.coefficients[0], 1e-9)
        assertEquals(2.0, fit.coefficients[1], 1e-9)
        assertEquals(3.0, fit.coefficients[2], 1e-9)
        assertEquals(2, fit.df)
        // A column that copies another can't be fitted.
        assertNull(leastSquares(rows.map { doubleArrayOf(it[0], it[1], it[1]) }, rows.map { it[1] }))
    }

    @Test fun `power means reward balance`() {
        assertEquals(0.5, powerMean(listOf(1.0, 0.0), 1.0), 1e-12)
        assertEquals(0.25, powerMean(listOf(1.0, 0.0), 0.5), 1e-12)
        assertEquals(0.5, powerMean(listOf(0.5, 0.5), 0.5), 1e-12)
        assertEquals(0.0, powerMean(listOf(0.0, 0.0), 0.5), 1e-12)
        assertEquals(1.0, powerMean(listOf(1.0, 1.0, 1.0), 0.5), 1e-12)
    }

    @Test fun `descriptive statistics`() {
        assertEquals(2.5, median(listOf(4.0, 1.0, 2.0, 3.0)), 1e-12)
        assertEquals(3.0, median(listOf(5.0, 1.0, 3.0)), 1e-12)
        assertEquals(1.0, sampleVariance(listOf(1.0, 2.0, 3.0)), 1e-12)
        assertEquals(0.0, sampleVariance(listOf(1.0)), 1e-12)
        assertEquals(Math.sqrt(2.0 / 3), populationStandardDeviation(listOf(1.0, 2.0, 3.0)), 1e-12)
    }

    @Test fun `ewma moves a share of the way and starts from the mean`() {
        assertEquals(2.0 / 22, ewmaFactor(21), 1e-12)
        // From the mean 15: 15 → 12.5 → 16.25.
        assertEquals(16.25, ewma(listOf(10.0, 20.0), factor = 0.5)!!, 1e-12)
        assertEquals(15.0, ewma(listOf(20.0), factor = 0.5, seed = 10.0)!!, 1e-12)
        assertNull(ewma(emptyList(), factor = 0.5))
    }

    @Test fun `decay halves every half-life`() {
        assertEquals(1.0, decayWeight(0.0, 7.0), 1e-12)
        assertEquals(0.5, decayWeight(7.0, 7.0), 1e-12)
        assertEquals(0.25, decayWeight(14.0, 7.0), 1e-12)
    }

    @Test fun `a baseline judges against the person's own spread`() {
        val steady = baseline(listOf(50.0, 52.0, 48.0, 50.0))!!
        assertEquals(50.0, steady.mean, 1e-12)
        assertTrue(steady.isBelow(47.0))
        assertFalse(steady.isBelow(49.0))
        assertTrue(steady.isAbove(53.0))
        // A baseline that never varied still needs the smallest spread before a value is unusual.
        val flat = baseline(listOf(50.0, 50.0, 50.0))!!
        assertFalse(flat.isBelow(49.0, minSpread = 2.0))
        assertTrue(flat.isBelow(47.0, minSpread = 2.0))
        assertNull(baseline(listOf(1.0, 2.0), minCount = 3))
    }
}
