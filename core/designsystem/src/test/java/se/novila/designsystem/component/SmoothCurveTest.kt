package se.novila.designsystem.component

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmoothCurveTest {
    private val xs = floatArrayOf(0f, 1f, 2f, 3f, 4f)

    @Test fun `a straight line keeps its slope`() {
        assertArrayEquals(floatArrayOf(2f, 2f, 2f, 2f, 2f), monotoneSlopes(xs, floatArrayOf(0f, 2f, 4f, 6f, 8f)), 1e-5f)
    }

    @Test fun `peaks and valleys are flat so the curve never overshoots`() {
        val slopes = monotoneSlopes(xs, floatArrayOf(6f, 9f, 5f, 8f, 8f))
        assertEquals(0f, slopes[1], 0f)
        assertEquals(0f, slopes[2], 0f)
        // A flat stretch stays flat.
        assertEquals(0f, slopes[3], 0f)
        assertEquals(0f, slopes[4], 0f)
    }

    @Test fun `steep rises are limited so the curve stays monotone`() {
        val slopes = monotoneSlopes(xs, floatArrayOf(0f, 0.1f, 10f, 10.1f, 10.2f))
        slopes.forEach { assertTrue(it >= 0f) }
        // Fritsch–Carlson: no tangent more than three times the secant beside it.
        assertTrue(slopes[1] <= 3f * 0.1f + 1e-5f)
    }

    @Test fun `fewer than two points have no slopes`() {
        assertEquals(0, monotoneSlopes(floatArrayOf(), floatArrayOf()).size)
        assertArrayEquals(floatArrayOf(0f), monotoneSlopes(floatArrayOf(1f), floatArrayOf(5f)), 0f)
    }
}
