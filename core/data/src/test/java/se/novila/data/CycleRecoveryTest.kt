package se.novila.data

import org.junit.Assert.assertEquals
import org.junit.Test
import se.novila.model.CycleLog
import se.novila.model.DayEntry
import se.novila.model.SleepFactor
import se.novila.recovery.RecoveryDay
import se.novila.recovery.RecoveryState
import se.novila.recovery.RecoveryStatus
import java.time.LocalDate

class CycleRecoveryTest {
    private val today = LocalDate.of(2026, 10, 22)

    /** A week of readings where the last three days have a few beats more and a lower HRV, as in a late half. */
    private val recovery = RecoveryState.Connected(
        (7L downTo 0L).map { daysAgo ->
            val late = daysAgo <= 2
            RecoveryDay(
                today.minusDays(daysAgo),
                restingHeartRate = if (late) 56 else 50,
                hrvMillis = if (late) 38 else 50,
            )
        },
    )

    /** A 28-day cycle whose last period began 16 days ago, so the late half began two days ago. */
    private val periods = CycleLog()
        .withPeriod(today.minusDays(44), on = true)
        .withPeriod(today.minusDays(16), on = true)

    @Test fun `without the cycle a normal late half reads as low recovery`() {
        assertEquals(RecoveryStatus.Low, recoveryStatus(recovery, today))
        // Logged but turned off counts for nothing.
        assertEquals(RecoveryStatus.Low, recoveryStatus(recovery, today, SavedCycle(enabled = false, log = periods)))
    }

    @Test fun `with the cycle the late half is compared only with itself`() {
        assertEquals(RecoveryStatus.Unknown, recoveryStatus(recovery, today, SavedCycle(enabled = true, log = periods)))
    }

    @Test fun `periods marked on Sleep count as well`() {
        val nights = listOf(today.minusDays(44), today.minusDays(16)).map {
            DayEntry(it).withSleepFactor(SleepFactor.Period, on = true)
        }
        assertEquals(RecoveryStatus.Unknown, recoveryStatus(recovery, today, SavedCycle(enabled = true), nights))
    }

    @Test fun `no readings is unknown`() {
        assertEquals(RecoveryStatus.Unknown, recoveryStatus(RecoveryState.Unavailable, today))
    }
}
