package se.novila.coach

import se.novila.exercise.LoadComparison
import se.novila.exercise.TrainingLoad
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.SleepDebt
import se.novila.recovery.RecoveryStatus

/**
 * The independent part of the body or the week a signal comes from. Last night and the sleep debt are both sleep, so
 * together they are one reason, not two: a calm day needs two different parts to agree.
 */
enum class ReadinessDomain { Sleep, Recovery, Load, Cycle }

/**
 * One thing the readiness is read from, and which way it pulls: [effect] -1 asks for a calmer day, +1 says the body
 * is ready, 0 says nothing either way. Each is a reason the user can read, never a hidden weight.
 */
sealed interface ReadinessSignal {
    val effect: Int

    /** The part the signal comes from; signals from the same part count once. */
    val domain: ReadinessDomain

    /** Last night against the sleep goal: more than [SHORT_NIGHT_MARGIN] short pulls down, otherwise up. */
    data class Night(val hours: Float, val short: Boolean) : ReadinessSignal {
        override val effect: Int get() = if (short) -1 else 1
        override val domain: ReadinessDomain get() = ReadinessDomain.Sleep
    }

    /**
     * The last two weeks' nights add up to a notable sleep debt ([SleepDebt.notable]): it pulls down even after a good
     * night, since one night doesn't pay it back (and then sleep pulls down, not up). No debt says nothing, since
     * last night already counts.
     */
    data class Debt(val minutes: Int) : ReadinessSignal {
        override val effect: Int get() = -1
        override val domain: ReadinessDomain get() = ReadinessDomain.Sleep
    }

    /** The watch's recovery against the user's own baseline. */
    data class Recovery(val low: Boolean) : ReadinessSignal {
        override val effect: Int get() = if (low) -1 else 1
        override val domain: ReadinessDomain get() = ReadinessDomain.Recovery
    }

    /** The last seven days' training load against the usual week, and whether the days were too alike. */
    data class Load(val comparison: LoadComparison, val uniform: Boolean) : ReadinessSignal {
        override val effect: Int
            get() = when {
                comparison == LoadComparison.Heavier || uniform -> -1
                comparison == LoadComparison.Lighter -> 1
                else -> 0
            }
        override val domain: ReadinessDomain get() = ReadinessDomain.Load
    }

    /** Symptoms logged today in *Follow your cycle*. */
    data object CycleSymptoms : ReadinessSignal {
        override val effect: Int get() = -1
        override val domain: ReadinessDomain get() = ReadinessDomain.Cycle
    }
}

/** What the day is ready for: a calm day, a day as planned, or a body that seems ready. */
enum class ReadinessLevel { Calm, Steady, Ready }

/**
 * Today's readiness, summed up from the [signals] that are known, each with its reason. The weighting is plain
 * counting of the parts ([ReadinessDomain]) the signals come from, so it can be said in words and one part can't
 * count twice (a short night and the sleep debt are both sleep):
 * - two or more parts that pull down: a calm day ([ReadinessLevel.Calm]),
 * - one: a day as planned, a little gentler ([ReadinessLevel.Steady]),
 * - none down and at least two up: ready ([ReadinessLevel.Ready]),
 * - otherwise steady.
 * A part pulls down when any of its signals does. Never a score without reasons: [reasons] are the signals behind
 * the level.
 */
data class Readiness(val level: ReadinessLevel, val signals: List<ReadinessSignal>) {
    /** The signals that decide the level: the ones pulling down, or for a ready day the ones pulling up. */
    val reasons: List<ReadinessSignal>
        get() = signals.filter { if (level == ReadinessLevel.Ready) it.effect > 0 else it.effect < 0 }
}

/** Parts that pull down before the day is a calm one. */
const val CALM_SIGNALS = 2

/** Parts that pull up, with none down, before the body seems ready. */
const val READY_SIGNALS = 2

/**
 * Today's readiness from last night ([day] and the sleep goal in [goals]), the two weeks' [sleepDebt], the watch's
 * [recovery], the last seven days' training [load] and whether the user logged [cycleSymptoms] today. What isn't known (no night logged, no
 * watch, too little training history, the cycle not followed) is left out, never counted as bad. Null when nothing is
 * known. Deterministic, so it is unit tested.
 */
fun readiness(
    day: DayEntry,
    goals: Goals = Goals(),
    recovery: RecoveryStatus = RecoveryStatus.Unknown,
    load: TrainingLoad? = null,
    cycleSymptoms: Boolean = false,
    sleepDebt: SleepDebt? = null,
): Readiness? {
    val signals = buildList {
        if (day.sleepHours > 0f) {
            add(ReadinessSignal.Night(day.sleepHours, short = day.sleepHours < goals.sleepHours - SHORT_NIGHT_MARGIN))
        }
        sleepDebt?.takeIf { it.notable }?.minutes?.let { add(ReadinessSignal.Debt(it)) }
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
    val byDomain = signals.groupBy { it.domain }.values
    val down = byDomain.count { part -> part.any { it.effect < 0 } }
    val up = byDomain.count { part -> part.none { it.effect < 0 } && part.any { it.effect > 0 } }
    val level = when {
        down >= CALM_SIGNALS -> ReadinessLevel.Calm
        down > 0 -> ReadinessLevel.Steady
        up >= READY_SIGNALS -> ReadinessLevel.Ready
        else -> ReadinessLevel.Steady
    }
    return Readiness(level, signals)
}
