package se.novila.recovery

import java.time.LocalDate

/**
 * What a watch recorded about the body's recovery on one day: [restingHeartRate] in beats per minute and heart
 * rate variability ([hrvMillis], RMSSD) in milliseconds. Either can be missing when the watch didn't measure it.
 * Like workouts and nights, it is read from the source and never saved by Novila.
 */
data class RecoveryDay(
    val date: LocalDate,
    val restingHeartRate: Int? = null,
    val hrvMillis: Long? = null,
) {
    /** Whether the watch measured nothing this day. */
    val isEmpty: Boolean get() = restingHeartRate == null && hrvMillis == null
}

/** What the app can show about recorded recovery, whatever the source is. */
sealed interface RecoveryState {
    /** The source isn't on this device, or not set up. Recovery stays unknown and the program is unchanged. */
    data object Unavailable : RecoveryState

    /**
     * Not connected yet. [denied] after the user said no: the source then stops asking, so access
     * can only be given in its own settings.
     */
    data class NeedsPermission(val denied: Boolean = false) : RecoveryState

    /** One day per date with what the watch measured, oldest first. */
    data class Connected(val days: List<RecoveryDay>) : RecoveryState
}

/**
 * Where recovery readings come from (Health Connect in the app). Like ExerciseSource, implementations never
 * throw: a missing sign-in or a network problem is a failed [Result], so the repository keeps what it showed.
 * Everything else is a [RecoveryState].
 */
fun interface RecoverySource {
    /** The days with recovery readings on [from] or later, oldest first. */
    suspend fun read(from: LocalDate): Result<RecoveryState>
}

/** How the body seems to have recovered. [Low] lets the training plan make today's session lighter. */
enum class RecoveryStatus { Unknown, Normal, Low }

/**
 * One [RecoveryDay] per date from the watch's samples, oldest first: [resting] beats per minute and [hrv]
 * milliseconds by the day they were measured. When the watch measured more than once a day, the last wins.
 */
fun recoveryDays(resting: Map<LocalDate, Int>, hrv: Map<LocalDate, Long>): List<RecoveryDay> =
    (resting.keys + hrv.keys).sorted().map { date ->
        RecoveryDay(date, restingHeartRate = resting[date], hrvMillis = hrv[date])
    }

/**
 * How the body has recovered, from the watch's [days] up to [today]: [RecoveryStatus.Low] when the latest day
 * is clearly worse than the user's own recent baseline, so the training plan can suggest an easier session
 * ([analyzeRecovery]'s result is what lowers the dose). Each metric is judged on its own and needs
 * [MIN_BASELINE_DAYS] earlier days to compare with; without that, or when the latest reading is older than
 * [STALE_DAYS] days, the status is [RecoveryStatus.Unknown]:
 * - Resting heart rate: [ELEVATED_BPM] or more above the baseline's median.
 * - HRV: less than [LOW_HRV_PERCENT] percent of the baseline's median.
 *
 * Only earlier days that are [comparable] with the latest one form the baseline. For a user who follows their cycle,
 * that is the days in the same half of it: resting heart rate normally rises a few beats and HRV falls in the late
 * half, so a baseline from the early half would read a normal cycle as poor recovery. Fewer than
 * [MIN_BASELINE_DAYS] such days leave the metric unjudged, never [RecoveryStatus.Low].
 * Deterministic and plain Kotlin, so it is unit tested. The source of the readings doesn't matter.
 */
fun analyzeRecovery(
    days: List<RecoveryDay>,
    today: LocalDate,
    comparable: (earlier: LocalDate, latest: LocalDate) -> Boolean = { _, _ -> true },
): RecoveryStatus {
    val measured = days.filter { it.date <= today && !it.isEmpty }.sortedBy { it.date }
    val current = measured.lastOrNull() ?: return RecoveryStatus.Unknown
    if (current.date < today.minusDays(STALE_DAYS)) return RecoveryStatus.Unknown
    val baseline = measured.filter {
        it.date >= current.date.minusDays(BASELINE_DAYS) && it.date < current.date && comparable(it.date, current.date)
    }
    val resting = impaired(
        current = current.restingHeartRate?.toDouble(),
        baseline = baseline.mapNotNull { it.restingHeartRate }.map(Int::toDouble),
    ) { value, median -> value >= median + ELEVATED_BPM }
    val hrv = impaired(
        current = current.hrvMillis?.toDouble(),
        baseline = baseline.mapNotNull { it.hrvMillis }.map(Long::toDouble),
    ) { value, median -> value * 100 < median * LOW_HRV_PERCENT }
    val judgements = listOfNotNull(resting, hrv)
    return when {
        judgements.isEmpty() -> RecoveryStatus.Unknown
        true in judgements -> RecoveryStatus.Low
        else -> RecoveryStatus.Normal
    }
}

/** Whether [current] is worse than the median of [baseline], or null when there is nothing to judge yet. */
private fun impaired(current: Double?, baseline: List<Double>, worse: (Double, Double) -> Boolean): Boolean? {
    if (current == null || baseline.size < MIN_BASELINE_DAYS) return null
    return worse(current, median(baseline))
}

/** The middle of sorted [values], the higher of the two middles for an even count. */
private fun median(values: List<Double>): Double = values.sorted()[values.size / 2]

/** A watch that hasn't synced for [STALE_DAYS] days says nothing about today. */
private const val STALE_DAYS = 2L

/** The days before the latest one that form the baseline to compare it with. */
private const val BASELINE_DAYS = 7L

/** How many baseline days each metric needs before it can be judged at all. */
private const val MIN_BASELINE_DAYS = 3

/** Resting heart rate this much above the baseline's median is worse. */
private const val ELEVATED_BPM = 5

/** HRV below this share of the baseline's median (percent) is worse. */
private const val LOW_HRV_PERCENT = 80
