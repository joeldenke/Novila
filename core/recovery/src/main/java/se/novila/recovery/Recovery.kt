package se.novila.recovery

import se.novila.science.Baseline
import se.novila.science.baseline
import java.time.LocalDate
import kotlin.math.ln

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
 * How the body has recovered, from the watch's [days] up to [today]: [RecoveryStatus.Low] when the last few days are
 * clearly worse than the user's own normal, so the training plan can suggest an easier session ([analyzeRecovery]'s
 * result is what lowers the dose). This follows how heart rate variability is read in sports science (Plews et al.
 * 2013, Buchheit 2014):
 * - The current value is the mean of the last [RECENT_DAYS] days, not one morning, since a single reading swings
 *   10–20 % from day to day.
 * - HRV (RMSSD) is skewed, so it is compared as ln(RMSSD).
 * - The normal is the user's own [Baseline] over the [BASELINE_DAYS] days before those, which don't overlap with them,
 *   and a value is worse when it is more than one of the user's own standard deviations out (at least
 *   [MIN_LN_HRV_SPREAD] for HRV and [MIN_BPM_SPREAD] beats for the resting heart rate, so a baseline that hardly
 *   varied doesn't call every small change unusual).
 *
 * Each metric is judged on its own and needs [MIN_BASELINE_DAYS] baseline days; without that, or when the latest
 * reading is older than [STALE_DAYS] days, the status is [RecoveryStatus.Unknown].
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
    val recentFrom = current.date.minusDays(RECENT_DAYS - 1)
    val recent = measured.filter { it.date >= recentFrom }
    val earlier = measured.filter {
        it.date < recentFrom && it.date >= recentFrom.minusDays(BASELINE_DAYS) && comparable(it.date, current.date)
    }
    val resting = judge(
        recent = recent.mapNotNull { it.restingHeartRate?.toDouble() },
        earlier = earlier.mapNotNull { it.restingHeartRate?.toDouble() },
    ) { value, normal -> normal.isAbove(value, minSpread = MIN_BPM_SPREAD) }
    val hrv = judge(
        recent = recent.mapNotNull { it.hrvMillis?.takeIf { ms -> ms > 0 }?.let { ms -> ln(ms.toDouble()) } },
        earlier = earlier.mapNotNull { it.hrvMillis?.takeIf { ms -> ms > 0 }?.let { ms -> ln(ms.toDouble()) } },
    ) { value, normal -> normal.isBelow(value, minSpread = MIN_LN_HRV_SPREAD) }
    val judgements = listOfNotNull(resting, hrv)
    return when {
        judgements.isEmpty() -> RecoveryStatus.Unknown
        true in judgements -> RecoveryStatus.Low
        else -> RecoveryStatus.Normal
    }
}

/**
 * Whether the mean of the [recent] values is worse than the [earlier] ones' baseline, or null when there is nothing
 * recent or too little to compare with.
 */
private fun judge(recent: List<Double>, earlier: List<Double>, worse: (Double, Baseline) -> Boolean): Boolean? {
    if (recent.isEmpty()) return null
    val normal = baseline(earlier, minCount = MIN_BASELINE_DAYS) ?: return null
    return worse(recent.average(), normal)
}

/** A watch that hasn't synced for [STALE_DAYS] days says nothing about today. */
private const val STALE_DAYS = 2L

/** The days averaged for the current value: the latest reading and the two before it. */
const val RECENT_DAYS = 3L

/** The days before the recent ones that form the baseline: the rest of the two weeks Health Connect gives. */
const val BASELINE_DAYS = 11L

/** How many baseline days each metric needs before it can be judged at all. */
const val MIN_BASELINE_DAYS = 4

/** The smallest spread of the resting heart rate, in beats per minute. */
const val MIN_BPM_SPREAD = 2.0

/** The smallest spread of ln(RMSSD): about 7 %, near what a watch measures from one morning to the next. */
const val MIN_LN_HRV_SPREAD = 0.07
