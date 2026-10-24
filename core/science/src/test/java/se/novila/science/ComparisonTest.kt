package se.novila.science

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ComparisonTest {
    @Test fun `welch's test uses the satterthwaite degrees of freedom`() {
        val test = welchTest(listOf(1.0, 2.0, 3.0, 4.0), listOf(2.0, 4.0, 6.0, 8.0, 10.0))!!
        // Means 2.5 and 6; variances 5/3 and 10; standard error sqrt(5/12 + 2).
        assertEquals(-3.5, test.estimate, 1e-12)
        assertEquals(Math.sqrt(5.0 / 12 + 2), test.standardError, 1e-12)
        val va = 5.0 / 12
        val vb = 2.0
        assertEquals((va + vb) * (va + vb) / (va * va / 3 + vb * vb / 4), test.df, 1e-12)
        assertTrue(test.p in 0.02..0.1)
    }

    @Test fun `groups that don't vary use the smallest standard error`() {
        val test = welchTest(listOf(7.0, 7.0, 7.0), listOf(8.0, 8.0, 8.0), minStandardError = 0.05)!!
        assertEquals(-20.0, test.t, 1e-12)
        assertEquals(4.0, test.df, 1e-12)
    }

    @Test fun `too few values say nothing`() {
        assertNull(welchTest(listOf(1.0), listOf(1.0, 2.0)))
    }

    @Test fun `the stratum takes away a difference it explains`() {
        // Sleep is an hour longer on weekends, and the habit mostly happens on weekends, but within each it changes
        // nothing.
        val weekday = listOf(7.0, 7.1, 6.9)
        val weekend = listOf(8.0, 8.1, 7.9, 8.0)
        val observations = weekday.map { Observation(it, exposed = false, stratum = false) } +
            listOf(7.0, 7.05).map { Observation(it, exposed = true, stratum = false) } +
            listOf(8.0, 8.05).map { Observation(it, exposed = false, stratum = true) } +
            weekend.map { Observation(it, exposed = true, stratum = true) }
        val raw = welchTest(
            observations.filter { it.exposed }.map { it.value },
            observations.filterNot { it.exposed }.map { it.value },
        )!!
        val adjusted = adjustedTest(observations)!!
        assertTrue(raw.estimate > 0.2)
        assertTrue(abs(adjusted.estimate) < 0.05)
        assertTrue(adjusted.p > 0.5)
    }

    @Test fun `a habit that only happens on weekends can't be told from the weekend`() {
        val observations = listOf(7.0, 7.1, 6.9).map { Observation(it, exposed = false, stratum = false) } +
            listOf(8.0, 8.1, 7.9).map { Observation(it, exposed = true, stratum = true) }
        assertNull(adjustedTest(observations))
    }

    @Test fun `without a varying stratum it is welch's test`() {
        val observations = listOf(7.0, 7.5, 6.5).map { Observation(it, exposed = true, stratum = false) } +
            listOf(6.0, 6.5, 5.5).map { Observation(it, exposed = false, stratum = false) }
        val adjusted = adjustedTest(observations)
        assertEquals(welchTest(listOf(7.0, 7.5, 6.5), listOf(6.0, 6.5, 5.5)), adjusted)
    }

    @Test fun `noticeable is about the size of the estimate`() {
        val test = TTest(estimate = -0.4, standardError = 0.1, t = -4.0, df = 10.0)
        assertTrue(test.isNoticeable(0.3))
        assertTrue(!test.isNoticeable(0.5))
    }
}
