package se.novila.feature.training

import se.novila.data.cardioType
import se.novila.exercise.ExerciseType
import se.novila.exercise.PlannedSession
import se.novila.model.CardioLog

/**
 * What the Training tab puts first for today, so the main action is always the one that matters: start today's
 * session, log today's cardio, or see what is next.
 */
sealed interface TodayPlan {
    /** A strength or rehab session to do today. */
    data class Workout(val session: PlannedSession) : TodayPlan

    /** Planned cardio that isn't logged or recorded yet: one tap logs it as planned. */
    data class Cardio(val session: PlannedSession, val log: CardioLog) : TodayPlan

    /** Mobility and stretching today, done in the user's own time. */
    data class Mobility(val session: PlannedSession) : TodayPlan

    /** Today's training is done, [minutes] in all. [next] is the next session this week, if any. */
    data class Done(val minutes: Int, val next: PlannedSession?) : TodayPlan

    /** Nothing planned today. [next] is the next session this week, if any. */
    data class Rest(val next: PlannedSession?) : TodayPlan
}

/** Today's plan from the week's program and what is done so far. */
fun TrainingUiState.todayPlan(): TodayPlan {
    val sessions = program.sessions.filter { it.day == today }
    val next = program.sessions.firstOrNull { it.day > today }
    val minutes = weekDone[today] ?: 0
    sessions.firstOrNull { it.exercises.isNotEmpty() }
        ?.takeIf { strengthMinutesToday == 0 }
        ?.let { return TodayPlan.Workout(it) }
    val cardio = sessions.firstOrNull { it.type.cardioType != null }
    val cardioType = cardio?.type?.cardioType
    if (cardio != null && cardioType != null && cardioToday.loggedMinutes == 0 && cardioToday.recordedMinutes == 0) {
        return TodayPlan.Cardio(cardio, CardioLog(cardioType, cardio.minutes))
    }
    sessions.firstOrNull { it.type == ExerciseType.Mobility }
        ?.takeIf { minutes == 0 }
        ?.let { return TodayPlan.Mobility(it) }
    return if (sessions.isNotEmpty() || minutes > 0) TodayPlan.Done(minutes, next) else TodayPlan.Rest(next)
}
