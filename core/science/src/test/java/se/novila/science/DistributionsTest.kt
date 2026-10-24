package se.novila.science

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.ln

class DistributionsTest {
    @Test fun `ln gamma matches factorials`() {
        assertEquals(ln(24.0), lnGamma(5.0), 1e-12)
        assertEquals(0.0, lnGamma(1.0), 1e-12)
        assertEquals(ln(Math.sqrt(Math.PI)), lnGamma(0.5), 1e-12)
    }

    @Test fun `t p-values match the tables`() {
        // Critical values for 5 %, two-sided.
        assertEquals(0.05, studentTTwoSidedP(12.706, 1.0), 1e-4)
        assertEquals(0.05, studentTTwoSidedP(2.447, 6.0), 1e-4)
        assertEquals(0.05, studentTTwoSidedP(2.086, 20.0), 1e-4)
        assertEquals(0.05, studentTTwoSidedP(1.95996, 1e7), 1e-4)
        // With six degrees of freedom, t = 2 is about the 9 % level, not 5 %.
        assertEquals(0.0924, studentTTwoSidedP(2.0, 6.0), 1e-3)
        assertEquals(1.0, studentTTwoSidedP(0.0, 6.0), 1e-12)
        assertEquals(studentTTwoSidedP(2.0, 6.0), studentTTwoSidedP(-2.0, 6.0), 1e-12)
    }

    @Test fun `chi-squared p-values match the tables`() {
        assertEquals(0.05, chiSquaredUpperP(3.8415, 1.0), 1e-4)
        assertEquals(0.05, chiSquaredUpperP(5.9915, 2.0), 1e-4)
        assertEquals(0.05, chiSquaredUpperP(12.5916, 6.0), 1e-4)
        assertEquals(0.01, chiSquaredUpperP(16.8119, 6.0), 1e-4)
        assertEquals(1.0, chiSquaredUpperP(0.0, 3.0), 1e-12)
    }
}
