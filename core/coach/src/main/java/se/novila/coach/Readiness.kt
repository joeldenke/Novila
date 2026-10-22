package se.novila.coach

import se.novila.exercise.LoadComparison
import se.novila.exercise.TrainingLoad
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.recovery.RecoveryStatus

/**
 * One thing the readiness is read from, and which way it pulls: [effect] -1 asks for a calmer day, +1 says the body
 * is ready, 0 says nothing either way. Each is a reason the user can read, never a hidden weight.
 */
sealed interface ReadinessSignal {
    val effect: Int

    /** Last night against the sleep goal: more than [SHORT_NIGHT_MARGIN] short pulls down, otherwise up. */
    data class Night(val hours: Float, val short: Boolean) : ReadinessSignal {
        override val effect: Int get() = if (short) -1 else 1
    }

    /** The watch's recovery against the user's own baseline. */
    data class Recovery(val low: Boolean) : ReadinessSignal {
        override val effect: Int get() = if (low) -1 else 1
    }

    /** The last seven days' training load against the usual week, and whether the days were too alike. */
    data class Load(val comparison: LoadComparison, val uniform: Boolean) : ReadinessSignal {
        override val effect: Int
            get() = when {
                comparison == LoadComparison.Heavier || uniform -> -1
                comparison == LoadComparison.Lighter -> 1
                else -> 0
            }
    }

    /** Symptoms logged today in *Follow your cycle*. */
    data object CycleSymptoms : ReadinessSignal {
        override val effect: Int get() = -1
    }
}

/** What the day is ready for: a calm day, a day as planned, or a body that seems ready. */
enum class ReadinessLevel { Calm, Steady, Ready }

/**
 * Today's readiness, summed up from the [signals] that are known, each with its reason. The weighting is plain
 * counting, so it can be said in words:
 * - two or more signals that pull down: a calm day ([ReadinessLevel.Calm]),
 * - one: a day as planned, a little gentler ([ReadinessLevel.Steady]),
 * - none down and at least two up: ready ([ReadinessLevel.Ready]),
 * - otherwise steady.
 * Never a score without reasons: [reasons] are the signals behind the level.
 */
data class Readiness(val level: ReadinessLevel, val signals: List<ReadinessSignal>) {
    /** The signals that decide the level: the ones pulling down, or for a ready day the ones pulling up. */
    val reasons: List<ReadinessSignal>
        get() = signals.filter { if (level == ReadinessLevel.Ready) it.effect > 0 else it.effect < 0 }
}

/** Signals that pull down before the day is a calm one. */
const val CALM_SIGNALS = 2

/** Signals that pull up, with none down, before the body seems ready. */
const val READY_SIGNALS = 2

/**
 * Today's readiness from last night ([day] and the sleep goal in [goals]), the watch's [recovery], the last seven
 * days' training [load] and whether the user logged [cycleSymptoms] today. What isn't known (no night logged, no
 * watch, too little training history, the cycle not followed) is left out, never counted as bad. Null when nothing is
 * known. Deterministic, so it is unit tested.
 */
fun readiness(
    day: DayEntry,
    goals: Goals = Goals(),
    recovery: RecoveryStatus = RecoveryStatus.Unknown,
    load: TrainingLoad? = null,
    cycleSymptoms: Boolean = false,
): Readiness? {
    val signals = buildList {
        if (day.sleepHours > 0f) {
            add(ReadinessSignal.Night(day.sleepHours, short = day.sleepHours < goals.sleepHours - SHORT_NIGHT_MARGIN))
        }
        when (recovery) {
            RecoveryStatus.Low -> add(ReadinessSignal.Recovery(low = true))
            RecoveryStatus.Normal -> add(ReadinessSignal.Recovery(low = false))
            RecoveryStatus.Unknown -> Unit
        }
        if (load != null && (load.comparison != LoadComparison.Unknown || load.uniform)) {
            add(ReadinessSignal.Load(load.comparison, load.uniform))
        }
        if (cycleSymptoms) add(ReadinessSignal.CycleSymptoms)
    }
    if (signals.isEmpty()) return null
    val down = signals.count { it.effect < 0 }
    val up = signals.count { it.effect > 0 }
    val level = when {
        down >= CALM_SIGNALS -> ReadinessLevel.Calm
        down > 0 -> ReadinessLevel.Steady
        up >= READY_SIGNALS -> ReadinessLevel.Ready
        else -> ReadinessLevel.Steady
    }
    return Readiness(level, signals)
}
