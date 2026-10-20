package se.novila.breathing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingTest {
    private val box = BreathingSession(
        id = "test",
        steps = listOf(
            BreathStep(BreathPhase.Inhale, 4),
            BreathStep(BreathPhase.HoldIn, 4),
            BreathStep(BreathPhase.Exhale, 4),
            BreathStep(BreathPhase.HoldOut, 4),
        ),
        rounds = 2,
    )

    @Test fun `duration is every step over every round`() {
        assertEquals(16_000L, box.cycleMillis)
        assertEquals(32_000L, box.totalMillis)
        assertEquals(32, box.totalSeconds)
    }

    @Test fun `the first moment is the start of the first inhale`() {
        val moment = box.momentAt(0L)!!
        assertEquals(1, moment.round)
        assertEquals(BreathPhase.Inhale, moment.phase)
        assertEquals(4, moment.secondsLeft)
        assertEquals(0f, moment.progress, 0.0001f)
    }

    @Test fun `each phase starts on its boundary`() {
        assertEquals(BreathPhase.Inhale, box.momentAt(0L)!!.phase)
        assertEquals(BreathPhase.Inhale, box.momentAt(3_999L)!!.phase)
        assertEquals(BreathPhase.HoldIn, box.momentAt(4_000L)!!.phase)
        assertEquals(BreathPhase.Exhale, box.momentAt(8_000L)!!.phase)
        assertEquals(BreathPhase.HoldOut, box.momentAt(12_000L)!!.phase)
        assertEquals(1, box.momentAt(15_999L)!!.round)
        assertEquals(2, box.momentAt(16_000L)!!.round)
    }

    @Test fun `the count goes down inside a phase`() {
        assertEquals(4, box.momentAt(0L)!!.secondsLeft)
        assertEquals(4, box.momentAt(1L)!!.secondsLeft)
        assertEquals(3, box.momentAt(1_000L)!!.secondsLeft)
        assertEquals(1, box.momentAt(3_000L)!!.secondsLeft)
        assertEquals(1, box.momentAt(3_999L)!!.secondsLeft)
    }

    @Test fun `progress runs from zero to almost one within a step`() {
        assertEquals(0.0f, box.momentAt(4_000L)!!.progress, 0.0001f)
        assertEquals(0.5f, box.momentAt(6_000L)!!.progress, 0.0001f)
        assertEquals(0.75f, box.momentAt(15_000L)!!.progress, 0.0001f)
    }

    @Test fun `the session ends after the last round`() {
        assertFalse(box.isFinished(31_999L))
        assertNull(box.momentAt(32_000L))
        assertTrue(box.isFinished(32_000L))
        assertEquals(0L, box.remainingMillis(33_000L))
        assertEquals(1_000L, box.remainingMillis(31_000L))
    }

    @Test fun `time before the start shows the first step at its beginning`() {
        val moment = box.momentAt(-500L)!!
        assertEquals(BreathPhase.Inhale, moment.phase)
        assertEquals(0f, moment.progress, 0.0001f)
        assertEquals(0L, moment.elapsedMillis)
    }

    @Test fun `the second round repeats the first round's steps`() {
        val first = box.momentAt(2_000L)!!
        val second = box.momentAt(18_000L)!!
        assertEquals(first.stepIndex, second.stepIndex)
        assertEquals(first.step, second.step)
        assertEquals(1, first.round)
        assertEquals(2, second.round)
    }

    @Test fun `the pacer grows on inhale, holds and shrinks on exhale`() {
        assertEquals(0.42f, breathScale(BreathPhase.Inhale, 0f), 0.0001f)
        assertEquals(0.9f, breathScale(BreathPhase.Inhale, 1f), 0.0001f)
        assertEquals(0.9f, breathScale(BreathPhase.HoldIn, 0.3f), 0.0001f)
        assertEquals(0.9f, breathScale(BreathPhase.Exhale, 0f), 0.0001f)
        assertEquals(0.42f, breathScale(BreathPhase.Exhale, 1f), 0.0001f)
        assertEquals(0.42f, breathScale(BreathPhase.HoldOut, 0.8f), 0.0001f)
    }

    @Test fun `progress outside the step is clamped`() {
        assertEquals(0.42f, breathScale(BreathPhase.Inhale, -3f), 0.0001f)
        assertEquals(0.9f, breathScale(BreathPhase.Inhale, 3f), 0.0001f)
    }

    @Test fun `the catalog is short, calm and easy to extend`() {
        assertTrue(BreathingCatalog.sessions.isNotEmpty())
        assertEquals(BreathingCatalog.sessions.size, BreathingCatalog.sessions.map { it.id }.toSet().size)
        BreathingCatalog.sessions.forEach { session ->
            assertTrue("$session is too long for a short break", session.totalSeconds in 30..180)
            assertTrue(session.steps.all { it.seconds in 2..10 })
            assertEquals(session, BreathingCatalog.session(session.id))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a step needs a positive length`() {
        BreathStep(BreathPhase.Inhale, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a session needs at least one round`() {
        BreathingSession(id = "broken", steps = listOf(BreathStep(BreathPhase.Inhale, 4)), rounds = 0)
    }
}
