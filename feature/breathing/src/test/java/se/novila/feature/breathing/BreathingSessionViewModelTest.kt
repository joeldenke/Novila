package se.novila.feature.breathing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.breathing.BreathPhase
import se.novila.breathing.BreathingCatalog
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** A clock the test steps by hand, so the session can be driven without waiting. */
private class SteppingClock(private var current: Instant = Instant.fromEpochMilliseconds(0)) : Clock {
    override fun now(): Instant = current

    fun advance(seconds: Long) {
        current += seconds.seconds
    }
}

@RunWith(RobolectricTestRunner::class)
class BreathingSessionViewModelTest {
    @Test fun `a session opens on its first breath`() {
        val clock = SteppingClock()
        val viewModel = BreathingSessionViewModel("box", clock)
        val state = viewModel.state.value

        assertEquals(BreathingCatalog.box, state.session)
        assertNotNull(state.moment)
        assertEquals(BreathPhase.Inhale, state.moment!!.phase)
        assertEquals(4, state.moment!!.secondsLeft)
        assertTrue(state.running)
    }

    @Test fun `an unknown id falls back to calm breathing`() {
        val viewModel = BreathingSessionViewModel("nope", SteppingClock())
        assertEquals(BreathingCatalog.calm, viewModel.state.value.session)
    }

    @Test fun `pausing keeps the moment and continuing keeps it from the start`() {
        val clock = SteppingClock()
        val viewModel = BreathingSessionViewModel("box", clock)
        clock.advance(5)
        viewModel.toggle()
        val paused = viewModel.state.value

        assertFalse(paused.running)
        // Five seconds in is the hold after the in-breath.
        assertEquals(BreathPhase.HoldIn, paused.moment!!.phase)
        assertEquals(3, paused.moment!!.secondsLeft)

        clock.advance(30)
        viewModel.toggle()
        assertTrue(viewModel.state.value.running)
        assertEquals(paused.moment, viewModel.state.value.moment)
    }

    @Test fun `restart goes back to the first breath`() {
        val clock = SteppingClock()
        val viewModel = BreathingSessionViewModel("box", clock)
        clock.advance(20)
        viewModel.restart()

        val moment = viewModel.state.value.moment!!
        assertEquals(BreathPhase.Inhale, moment.phase)
        assertEquals(0L, viewModel.state.value.elapsedMillis)
        assertTrue(viewModel.state.value.running)
    }

    @Test fun `a finished session reports no moment and nothing left`() {
        val clock = SteppingClock()
        val viewModel = BreathingSessionViewModel("calm", clock)
        // Past every round of calm breathing.
        clock.advance(100)
        viewModel.toggle()
        val state = viewModel.state.value
        assertNull(state.moment)
        assertEquals(0, state.remainingSeconds)
        assertTrue(state.finished)
    }
}
