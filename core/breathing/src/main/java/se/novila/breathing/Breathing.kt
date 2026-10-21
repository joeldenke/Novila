package se.novila.breathing

/** The four parts a guided breath can have. A session may leave out either hold. */
enum class BreathPhase { Inhale, HoldIn, Exhale, HoldOut }

/** One part of a breath, in whole seconds. */
data class BreathStep(val phase: BreathPhase, val seconds: Int) {
    init {
        require(seconds > 0) { "A breath step needs a positive length, was $seconds s" }
    }

    val millis: Long get() = seconds * 1000L
}

/**
 * A guided breathing session: a few [steps] repeated for [rounds]. The data is the whole session, so it can be
 * tested, shown and extended without a timer of its own; [momentAt] maps elapsed time to where the user is.
 */
data class BreathingSession(
    val id: String,
    val steps: List<BreathStep>,
    val rounds: Int,
) {
    init {
        require(id.isNotBlank()) { "A session needs an id" }
        require(steps.isNotEmpty()) { "A session needs at least one step" }
        require(rounds > 0) { "A session needs at least one round, was $rounds" }
    }

    /** One full breath, in, holds and out. */
    val cycleMillis: Long = steps.sumOf { it.millis }

    /** The whole session, all rounds. */
    val totalMillis: Long = cycleMillis * rounds

    /** The session rounded up to whole seconds, for a duration label. */
    val totalSeconds: Int = ((totalMillis + 999L) / 1000L).toInt()

    /**
     * Where the user is after [elapsedMillis], or null when the session is over. Time before the start shows the
     * first step at its beginning, so a fresh session always has a phase to show.
     */
    fun momentAt(elapsedMillis: Long): BreathingMoment? {
        if (elapsedMillis >= totalMillis) return null
        val elapsed = elapsedMillis.coerceAtLeast(0L)
        val round = (elapsed / cycleMillis).toInt() + 1
        var withinCycle = elapsed % cycleMillis
        steps.forEachIndexed { index, step ->
            if (withinCycle < step.millis) {
                return BreathingMoment(
                    round = round,
                    stepIndex = index,
                    step = step,
                    elapsedInStepMillis = withinCycle,
                    remainingInStepMillis = step.millis - withinCycle,
                    progress = (withinCycle.toDouble() / step.millis).toFloat(),
                    elapsedMillis = elapsed,
                )
            }
            withinCycle -= step.millis
        }
        // Only a rounding error in the steps above can get here; the last step is the safe answer.
        val last = steps.lastIndex
        return BreathingMoment(
            round = round,
            stepIndex = last,
            step = steps[last],
            elapsedInStepMillis = 0L,
            remainingInStepMillis = steps[last].millis,
            progress = 0f,
            elapsedMillis = elapsed,
        )
    }

    /** Milliseconds left of the whole session after [elapsedMillis], never negative. */
    fun remainingMillis(elapsedMillis: Long): Long = (totalMillis - elapsedMillis).coerceAtLeast(0L)

    fun isFinished(elapsedMillis: Long): Boolean = elapsedMillis >= totalMillis
}

/** A point inside a [BreathingSession], as the screen shows it. */
data class BreathingMoment(
    /** 1-based, so the screen can say "round 2 of 5". */
    val round: Int,
    val stepIndex: Int,
    val step: BreathStep,
    val elapsedInStepMillis: Long,
    val remainingInStepMillis: Long,
    /** How far into the step, 0f..1f. */
    val progress: Float,
    val elapsedMillis: Long,
) {
    val phase: BreathPhase get() = step.phase

    /** Whole seconds left of this part, rounded up so the first second shows the full length. */
    val secondsLeft: Int get() = ((remainingInStepMillis + 999L) / 1000L).toInt()
}

/**
 * How large the pacer's breath circle is at [progress] through [phase], between [min] and [max]. Inhale grows,
 * exhale shrinks and both holds stay still. Deterministic, so the drawing is testable and calm.
 */
fun breathScale(
    phase: BreathPhase,
    progress: Float,
    min: Float = 0.42f,
    max: Float = 0.9f,
): Float {
    val p = progress.coerceIn(0f, 1f)
    return when (phase) {
        BreathPhase.Inhale -> min + (max - min) * p
        BreathPhase.Exhale -> max - (max - min) * p
        BreathPhase.HoldIn -> max
        BreathPhase.HoldOut -> min
    }
}

/** The sessions Novila offers. Short, calm and easy to learn; add a session here to make it available. */
object BreathingCatalog {
    /** Calm breathing: a slightly longer out-breath, the easiest place to start. */
    val calm = BreathingSession(
        id = "calm",
        steps = listOf(
            BreathStep(BreathPhase.Inhale, 4),
            BreathStep(BreathPhase.Exhale, 6),
        ),
        rounds = 6,
    )

    /** Box breathing: four equal parts, used to settle before rest. */
    val box = BreathingSession(
        id = "box",
        steps = listOf(
            BreathStep(BreathPhase.Inhale, 4),
            BreathStep(BreathPhase.HoldIn, 4),
            BreathStep(BreathPhase.Exhale, 4),
            BreathStep(BreathPhase.HoldOut, 4),
        ),
        rounds = 5,
    )

    /** 4-7-8: a long, slow out-breath for winding down before bed. */
    val fourSevenEight = BreathingSession(
        id = "four-seven-eight",
        steps = listOf(
            BreathStep(BreathPhase.Inhale, 4),
            BreathStep(BreathPhase.HoldIn, 7),
            BreathStep(BreathPhase.Exhale, 8),
        ),
        rounds = 4,
    )

    val sessions: List<BreathingSession> = listOf(calm, box, fourSevenEight)

    fun session(id: String): BreathingSession? = sessions.firstOrNull { it.id == id }
}
