package se.novila.coach

import java.time.LocalDate

/**
 * What the user said after one strength or rehab session: [effort] on the Borg CR10 scale (RPE 1–10) and [pain]
 * (0–10), each null when not given. [rehabPhase] is the rehab phase the session was done in, 0 for other sessions.
 */
data class SessionFeedback(val date: LocalDate, val effort: Int?, val pain: Int?, val rehabPhase: Int = 0)

/** How the coach changes the dose of the week's strength sessions after the latest ones. */
enum class EffortAdvice {
    /** One set less per exercise: the sessions were very hard, or hurt. */
    Easier,

    /** The dose is right. */
    Same,

    /** Two repetitions (or ten seconds) more per set: the sessions were easy. */
    Harder,
}

/** What the coach suggests for a rehab program after the latest sessions in the current phase. */
enum class RehabAdvice {
    /** The last sessions were calm: move on to the next phase. */
    MoveOn,

    /** Calm so far, but not enough sessions yet to move on. */
    KeepGoing,

    /** It hurt a bit more than it should: stay in the phase until it settles. */
    Stay,

    /** It hurt too much: go back to the previous phase. */
    StepBack,

    /** It hurt too much already in the first phase: rest and see a physiotherapist. */
    SeekCare,

    /** The last phase is calm: the program is done, ordinary training can take over. */
    Done,
}

/** The effort advice, from the latest sessions' average [effort] (rounded) or their [pain]. */
data class EffortCheck(val advice: EffortAdvice, val effort: Int?, val pain: Int?)

/** The rehab advice and the [phase] it points to; [calmSessions] counts towards [CoachFeedback.CALM_SESSIONS]. */
data class RehabCheck(val advice: RehabAdvice, val phase: Int, val calmSessions: Int, val pain: Int?)

/**
 * Reads the user's feedback after their sessions, so the coach adjusts the dose and suggests the rehab phase itself.
 * The rules follow the pain monitoring model physiotherapists use: pain up to 3 of 10 during a session is fine,
 * 4–5 means wait, and more than 5 means too much. The coach only suggests a new phase; the user changes it.
 * Plain Kotlin and deterministic, so it is unit tested.
 */
object CoachFeedback {
    const val EFFORT_DAYS = 14L
    const val REHAB_DAYS = 21L
    const val CALM_SESSIONS = 3
    const val CALM_PAIN = 3
    const val TOO_MUCH_PAIN = 6
    const val HARD_EFFORT = 9
    const val EASY_EFFORT = 4

    /** A calm rehab session must not have been close to the limit either. */
    private const val CALM_EFFORT = 7
    private const val EFFORT_SESSIONS = 2

    /**
     * The dose for the week's strength sessions, from the latest ones (rehab aside) in the last [EFFORT_DAYS] days.
     * Null until there is something to go on: pain after the latest session, or the effort of two sessions.
     */
    fun effort(feedback: List<SessionFeedback>, today: LocalDate): EffortCheck? {
        val recent = recent(feedback, today, EFFORT_DAYS).filter { it.rehabPhase == 0 }
        val pain = recent.firstOrNull()?.pain
        if (pain != null && pain >= TOO_MUCH_PAIN) return EffortCheck(EffortAdvice.Easier, effort = null, pain = pain)
        val efforts = recent.mapNotNull { it.effort?.takeIf { effort -> effort > 0 } }.take(EFFORT_SESSIONS)
        if (efforts.size < EFFORT_SESSIONS) return null
        val average = Math.round(efforts.average()).toInt()
        val advice = when {
            average >= HARD_EFFORT -> EffortAdvice.Easier
            average <= EASY_EFFORT -> EffortAdvice.Harder
            else -> EffortAdvice.Same
        }
        return EffortCheck(advice, effort = average, pain = null)
    }

    /**
     * The next step for a rehab program in [phase], from the sessions done in that phase since the user last did a
     * session in another one, in the last [REHAB_DAYS] days. Null until a session in the phase has a pain answer.
     */
    fun rehab(feedback: List<SessionFeedback>, phase: Int, today: LocalDate): RehabCheck? {
        val inPhase = recent(feedback, today, REHAB_DAYS)
            .filter { it.rehabPhase > 0 }
            .takeWhile { it.rehabPhase == phase }
            .filter { it.pain != null }
        val latest = inPhase.firstOrNull()?.pain ?: return null
        val calm = inPhase.takeWhile { it.pain!! <= CALM_PAIN && (it.effort ?: 0) <= CALM_EFFORT }.size
        return when {
            latest >= TOO_MUCH_PAIN && phase > 1 -> RehabCheck(RehabAdvice.StepBack, phase - 1, calm, latest)
            latest >= TOO_MUCH_PAIN -> RehabCheck(RehabAdvice.SeekCare, phase, calm, latest)
            latest > CALM_PAIN -> RehabCheck(RehabAdvice.Stay, phase, calm, latest)
            calm < CALM_SESSIONS -> RehabCheck(RehabAdvice.KeepGoing, phase, calm, latest)
            phase < CoachPreferences.MAX_REHAB_PHASE -> RehabCheck(RehabAdvice.MoveOn, phase + 1, calm, latest)
            else -> RehabCheck(RehabAdvice.Done, phase, calm, latest)
        }
    }

    /** Feedback from the last [days] days up to [today], newest first. */
    private fun recent(feedback: List<SessionFeedback>, today: LocalDate, days: Long) = feedback
        .filter { it.date <= today && it.date > today.minusDays(days) }
        .sortedByDescending { it.date }
}
