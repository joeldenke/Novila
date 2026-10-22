package se.novila.data

import se.novila.model.DayEntry
import se.novila.model.SleepFactor
import se.novila.model.cyclePhase
import se.novila.recovery.RecoveryState
import se.novila.recovery.RecoveryStatus
import se.novila.recovery.analyzeRecovery
import java.time.LocalDate

/**
 * How the body has recovered on [today], from the watch's [recovery]. For a user who follows their [cycle], a day is
 * compared only with days in the same half of it ([cyclePhase]), from the period days they logged and the nights they
 * marked with a period on Sleep ([days]), so a normal cycle never reads as poor recovery.
 */
fun recoveryStatus(
    recovery: RecoveryState,
    today: LocalDate,
    cycle: SavedCycle = SavedCycle(),
    days: List<DayEntry> = emptyList(),
): RecoveryStatus {
    val readings = (recovery as? RecoveryState.Connected)?.days ?: return RecoveryStatus.Unknown
    if (!cycle.enabled) return analyzeRecovery(readings, today)
    val periods = cycle.periodDays + days.filter { SleepFactor.Period in it.sleepFactors }.map { it.date }
    return analyzeRecovery(readings, today) { earlier, latest ->
        cyclePhase(periods, earlier) == cyclePhase(periods, latest)
    }
}
