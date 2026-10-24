package se.novila.recovery

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecoveryAnalysisTest {
    private val today = LocalDate.of(2026, 10, 15)

    /**
     * Two weeks of readings: a steady resting heart rate of 50 and HRV of 50 ms in the baseline (3 to 13 days ago),
     * and [recent] for the last three days (2 days ago, yesterday, today).
     */
    private fun days(recent: (daysAgo: Long) -> RecoveryDay = { steady(it) }): List<RecoveryDay> =
        (3L..13L).map { steady(it) } + (2L downTo 0L).map(recent)

    private fun steady(daysAgo: Long) = RecoveryDay(today.minusDays(daysAgo), restingHeartRate = 50, hrvMillis = 50)

    @Test fun `no readings is unknown`() {
        assertEquals(RecoveryStatus.Unknown, analyzeRecovery(emptyList(), today))
        assertEquals(RecoveryStatus.Unknown, analyzeRecovery(listOf(RecoveryDay(today)), today))
    }

    @Test fun `steady values are normal`() {
        assertEquals(RecoveryStatus.Normal, analyzeRecovery(days(), today))
    }

    @Test fun `a resting heart rate above the normal range for some days is low`() {
        val days = days { RecoveryDay(today.minusDays(it), restingHeartRate = 53, hrvMillis = 50) }
        assertEquals(RecoveryStatus.Low, analyzeRecovery(days, today))
    }

    @Test fun `a resting heart rate within the smallest spread is still normal`() {
        val days = days { RecoveryDay(today.minusDays(it), restingHeartRate = 52, hrvMillis = 50) }
        assertEquals(RecoveryStatus.Normal, analyzeRecovery(days, today))
    }

    @Test fun `one odd morning doesn't decide, the last days together do`() {
        val days = days { daysAgo ->
            if (daysAgo == 0L) RecoveryDay(today, restingHeartRate = 55, hrvMillis = 45) else steady(daysAgo)
        }
        assertEquals(RecoveryStatus.Normal, analyzeRecovery(days, today))
    }

    @Test fun `an HRV below the normal range is low, compared as ln(RMSSD)`() {
        // 50 ms times e^-0.07 is about 46.6 ms.
        val low = days { RecoveryDay(today.minusDays(it), restingHeartRate = 50, hrvMillis = 46) }
        assertEquals(RecoveryStatus.Low, analyzeRecovery(low, today))
        val normal = days { RecoveryDay(today.minusDays(it), restingHeartRate = 50, hrvMillis = 47) }
        assertEquals(RecoveryStatus.Normal, analyzeRecovery(normal, today))
    }

    @Test fun `the normal range is the user's own spread`() {
        // A baseline that varies between 40 and 60 ms: 44 ms for three days is within it.
        val varied = (3L..13L).map { daysAgo ->
            RecoveryDay(today.minusDays(daysAgo), restingHeartRate = 50, hrvMillis = if (daysAgo % 2 == 0L) 40 else 60)
        }
        val recent = (2L downTo 0L).map { RecoveryDay(today.minusDays(it), restingHeartRate = 50, hrvMillis = 44) }
        assertEquals(RecoveryStatus.Normal, analyzeRecovery(varied + recent, today))
        // The same 44 ms against a steady 50 is low.
        assertEquals(
            RecoveryStatus.Low,
            analyzeRecovery(days { RecoveryDay(today.minusDays(it), restingHeartRate = 50, hrvMillis = 44) }, today),
        )
    }

    @Test fun `one bad metric is enough for low`() {
        val days = days { RecoveryDay(today.minusDays(it), restingHeartRate = 50, hrvMillis = 30) }
        assertEquals(RecoveryStatus.Low, analyzeRecovery(days, today))
    }

    @Test fun `a metric without enough baseline days is not judged`() {
        val days = (3L..5L).map { steady(it) } + RecoveryDay(today, restingHeartRate = 70, hrvMillis = 20)
        assertEquals(RecoveryStatus.Unknown, analyzeRecovery(days, today))
    }

    @Test fun `one metric with a baseline is enough to judge the day`() {
        val days = (3L..7L).map { RecoveryDay(today.minusDays(it), hrvMillis = 50) } +
            RecoveryDay(today, hrvMillis = 30)
        assertEquals(RecoveryStatus.Low, analyzeRecovery(days, today))
    }

    @Test fun `a latest reading older than two days is stale`() {
        val days = (3L..13L).map { steady(it) }
        assertEquals(RecoveryStatus.Unknown, analyzeRecovery(days, today))
    }

    @Test fun `readings after today are ignored`() {
        val days = days() + RecoveryDay(today.plusDays(1), restingHeartRate = 90, hrvMillis = 10)
        assertEquals(RecoveryStatus.Normal, analyzeRecovery(days, today))
    }

    @Test fun `one day per date, the last sample of a day wins`() {
        val resting = mapOf(
            LocalDate.of(2026, 10, 14) to 52,
            LocalDate.of(2026, 10, 15) to 50,
            LocalDate.of(2026, 10, 15) to 51,
        )
        val hrv = mapOf(LocalDate.of(2026, 10, 15) to 48L)
        assertEquals(
            listOf(
                RecoveryDay(LocalDate.of(2026, 10, 14), restingHeartRate = 52),
                RecoveryDay(LocalDate.of(2026, 10, 15), restingHeartRate = 51, hrvMillis = 48),
            ),
            recoveryDays(resting, hrv),
        )
    }

    @Test fun `only comparable days form the baseline, and too few of them say nothing`() {
        // From [lateFrom] on the days are in the late half of the cycle, with a few beats more and a lower HRV; the
        // days before are in the early half.
        fun days(lateFrom: Long) = (0L..13L).map { daysAgo ->
            if (daysAgo <= lateFrom) {
                RecoveryDay(today.minusDays(daysAgo), restingHeartRate = 56, hrvMillis = 38)
            } else {
                steady(daysAgo)
            }
        }
        fun sameHalf(lateFrom: Long) = { earlier: LocalDate, latest: LocalDate ->
            val late = today.minusDays(lateFrom)
            (earlier >= late) == (latest >= late)
        }
        // Against the whole two weeks the late half looks like poor recovery.
        assertEquals(RecoveryStatus.Low, analyzeRecovery(days(lateFrom = 4), today))
        // Against its own half there are only two days before the recent ones, so nothing is judged.
        assertEquals(RecoveryStatus.Unknown, analyzeRecovery(days(lateFrom = 4), today, sameHalf(lateFrom = 4)))
        // With six late days before the recent ones, the same readings are normal for that half.
        assertEquals(RecoveryStatus.Normal, analyzeRecovery(days(lateFrom = 8), today, sameHalf(lateFrom = 8)))
    }
}
