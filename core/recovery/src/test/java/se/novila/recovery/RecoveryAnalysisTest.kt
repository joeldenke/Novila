package se.novila.recovery

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecoveryAnalysisTest {
    private val today = LocalDate.of(2026, 10, 15)

    /** Seven days of a steady resting heart rate of 50 and HRV of 50 ms before [latest]. */
    private fun baseline(latest: (Int, Long) -> RecoveryDay): List<RecoveryDay> =
        (1L..7L).map { daysAgo ->
            RecoveryDay(today.minusDays(daysAgo), restingHeartRate = 50, hrvMillis = 50)
        } + latest(50, 50)

    @Test fun `no readings is unknown`() {
        assertEquals(RecoveryStatus.Unknown, analyzeRecovery(emptyList(), today))
        assertEquals(RecoveryStatus.Unknown, analyzeRecovery(listOf(RecoveryDay(today)), today))
    }

    @Test fun `steady values are normal`() {
        val days = baseline { resting, hrv -> RecoveryDay(today, restingHeartRate = resting, hrvMillis = hrv) }
        assertEquals(RecoveryStatus.Normal, analyzeRecovery(days, today))
    }

    @Test fun `a resting heart rate clearly above the baseline is low`() {
        val days = baseline { _, hrv -> RecoveryDay(today, restingHeartRate = 55, hrvMillis = hrv) }
        assertEquals(RecoveryStatus.Low, analyzeRecovery(days, today))
    }

    @Test fun `a resting heart rate just under the threshold is still normal`() {
        val days = baseline { _, hrv -> RecoveryDay(today, restingHeartRate = 54, hrvMillis = hrv) }
        assertEquals(RecoveryStatus.Normal, analyzeRecovery(days, today))
    }

    @Test fun `an HRV clearly below the baseline is low`() {
        val days = baseline { resting, _ -> RecoveryDay(today, restingHeartRate = resting, hrvMillis = 39) }
        assertEquals(RecoveryStatus.Low, analyzeRecovery(days, today))
    }

    @Test fun `an HRV at eighty percent of the baseline is still normal`() {
        val days = baseline { resting, _ -> RecoveryDay(today, restingHeartRate = resting, hrvMillis = 40) }
        assertEquals(RecoveryStatus.Normal, analyzeRecovery(days, today))
    }

    @Test fun `one bad metric is enough for low`() {
        val days = baseline { _, _ -> RecoveryDay(today, restingHeartRate = 60, hrvMillis = 30) }
        assertEquals(RecoveryStatus.Low, analyzeRecovery(days, today))
    }

    @Test fun `the baseline is the median, so one odd day doesn't decide`() {
        val days = (1L..6L).map { daysAgo ->
            RecoveryDay(today.minusDays(daysAgo), restingHeartRate = 50, hrvMillis = 50)
        } + RecoveryDay(today.minusDays(7), restingHeartRate = 80, hrvMillis = 10) +
            RecoveryDay(today, restingHeartRate = 54, hrvMillis = 45)
        assertEquals(RecoveryStatus.Normal, analyzeRecovery(days, today))
    }

    @Test fun `a metric without enough baseline days is not judged`() {
        val days = (1L..2L).map { daysAgo ->
            RecoveryDay(today.minusDays(daysAgo), restingHeartRate = 50, hrvMillis = 50)
        } + RecoveryDay(today, restingHeartRate = 70, hrvMillis = 20)
        assertEquals(RecoveryStatus.Unknown, analyzeRecovery(days, today))
    }

    @Test fun `one metric with a baseline is enough to judge the day`() {
        val days = (1L..4L).map { daysAgo -> RecoveryDay(today.minusDays(daysAgo), hrvMillis = 50) } +
            RecoveryDay(today, hrvMillis = 30)
        assertEquals(RecoveryStatus.Low, analyzeRecovery(days, today))
    }

    @Test fun `a latest reading older than two days is stale`() {
        val days = (3L..9L).map { daysAgo ->
            RecoveryDay(today.minusDays(daysAgo), restingHeartRate = 50, hrvMillis = 50)
        }
        assertEquals(RecoveryStatus.Unknown, analyzeRecovery(days, today))
    }

    @Test fun `readings after today are ignored`() {
        val days = (1L..7L).map { daysAgo ->
            RecoveryDay(today.minusDays(daysAgo), restingHeartRate = 50, hrvMillis = 50)
        } + RecoveryDay(today.plusDays(1), restingHeartRate = 90, hrvMillis = 10)
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
}
