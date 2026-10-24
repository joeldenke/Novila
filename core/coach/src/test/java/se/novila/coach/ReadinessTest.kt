package se.novila.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.novila.exercise.TrainingLoad
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.SleepDebt
import se.novila.recovery.RecoveryStatus
import java.time.LocalDate

class ReadinessTest {
    private val date = LocalDate.of(2026, 10, 22)

    private fun night(hours: Float) = DayEntry(date, sleepHours = hours)

    /** A week 40 % heavier than the three before. */
    private val heavy = TrainingLoad(week = 1400, usualWeek = 1000, monotony = 1.0, sessions = 4, rated = 2)
    private val usual = TrainingLoad(week = 1000, usualWeek = 1000, monotony = 1.0, sessions = 3, rated = 1)

    @Test fun `nothing known gives no readiness`() {
        assertNull(readiness(DayEntry(date)))
        // A load without a usual week to compare with says nothing either.
        assertNull(readiness(DayEntry(date), load = usual.copy(usualWeek = null)))
    }

    @Test fun `two signals that pull down ask for a calm day, with both as reasons`() {
        val readiness = readiness(night(5.5f), Goals(), load = heavy)!!
        assertEquals(ReadinessLevel.Calm, readiness.level)
        assertEquals(
            listOf(ReadinessSignal.Night(5.5f, short = true), ReadinessSignal.Load(heavy.comparison, uniform = false)),
            readiness.reasons,
        )
    }

    @Test fun `one signal that pulls down is a day as planned, a little gentler`() {
        val readiness = readiness(night(5.5f), Goals(), RecoveryStatus.Normal)!!
        assertEquals(ReadinessLevel.Steady, readiness.level)
        assertEquals(listOf(ReadinessSignal.Night(5.5f, short = true)), readiness.reasons)
    }

    @Test fun `a good night and a normal recovery are ready`() {
        val readiness = readiness(night(8f), Goals(), RecoveryStatus.Normal, usual)!!
        assertEquals(ReadinessLevel.Ready, readiness.level)
        // A usual week neither helps nor hurts, so it is no reason.
        assertEquals(
            listOf(ReadinessSignal.Night(8f, short = false), ReadinessSignal.Recovery(low = false)),
            readiness.reasons,
        )
    }

    @Test fun `a good night alone is steady, not ready`() {
        assertEquals(ReadinessLevel.Steady, readiness(night(8f))!!.level)
    }

    @Test fun `symptoms and a low recovery ask for a calm day even after a good night`() {
        val readiness = readiness(night(8f), Goals(), RecoveryStatus.Low, cycleSymptoms = true)!!
        assertEquals(ReadinessLevel.Calm, readiness.level)
        assertEquals(listOf(ReadinessSignal.Recovery(low = true), ReadinessSignal.CycleSymptoms), readiness.reasons)
    }

    @Test fun `a short night and the sleep debt are one part, not two`() {
        val debt = SleepDebt(minutes = 300, nights = 14)
        val readiness = readiness(night(5.5f), Goals(sleepHours = 8f), sleepDebt = debt)!!
        assertEquals(ReadinessLevel.Steady, readiness.level)
        assertEquals(listOf(ReadinessSignal.Night(5.5f, short = true), ReadinessSignal.Debt(300)), readiness.reasons)
        // A second part that pulls down makes it a calm day.
        assertEquals(
            ReadinessLevel.Calm,
            readiness(night(5.5f), Goals(sleepHours = 8f), load = heavy, sleepDebt = debt)!!.level,
        )
    }

    @Test fun `a good night with a sleep debt doesn't count sleep as ready`() {
        val debt = SleepDebt(minutes = 300, nights = 14)
        val readiness = readiness(night(8f), Goals(sleepHours = 8f), RecoveryStatus.Normal, usual, sleepDebt = debt)!!
        // Sleep pulls down through the debt, recovery up: a day as planned, not a ready one.
        assertEquals(ReadinessLevel.Steady, readiness.level)
    }

    @Test fun `a uniform week pulls down even without a usual week`() {
        val uniform = usual.copy(usualWeek = null, monotony = 4.0)
        assertEquals(
            listOf(ReadinessSignal.Load(uniform.comparison, uniform = true)),
            readiness(DayEntry(date), load = uniform)!!.reasons,
        )
    }

    @Test fun `a notable sleep debt pulls down even after a good night`() {
        val debt = SleepDebt(minutes = 300, nights = 14)
        val readiness = readiness(night(8f), Goals(sleepHours = 8f), RecoveryStatus.Low, sleepDebt = debt)!!
        assertEquals(ReadinessLevel.Calm, readiness.level)
        assertEquals(listOf(ReadinessSignal.Debt(300), ReadinessSignal.Recovery(low = true)), readiness.reasons)

        // A small debt, or one not known yet, says nothing.
        val small = readiness(night(8f), Goals(sleepHours = 8f), sleepDebt = SleepDebt(minutes = 60, nights = 14))!!
        assertEquals(listOf(ReadinessSignal.Night(8f, short = false)), small.signals)
        val unknown = readiness(night(8f), Goals(sleepHours = 8f), sleepDebt = SleepDebt(minutes = null, nights = 3))!!
        assertEquals(listOf(ReadinessSignal.Night(8f, short = false)), unknown.signals)
    }
}
