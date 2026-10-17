package se.novila.feature.training

import org.junit.Assert.assertEquals
import org.junit.Test
import se.novila.exercise.ExerciseType
import se.novila.exercise.PlannedSession
import se.novila.model.CardioLog
import se.novila.model.CardioType
import java.time.DayOfWeek

class TodayPlanTest {
    private val state = TrainingUiState(
        program = TrainingPreviewData.program,
        coach = TrainingPreviewData.newCoach,
        today = DayOfWeek.MONDAY,
        strengthMinutesToday = 0,
    )
    private val sessions = state.program.sessions
    private fun on(day: DayOfWeek) = sessions.first { it.day == day }

    @Test fun `a strength day starts with the session`() {
        val monday = on(DayOfWeek.MONDAY)
        assertEquals(ExerciseType.Strength, monday.type)
        assertEquals(TodayPlan.Workout(monday), state.todayPlan())
    }

    @Test fun `a done session shows the next one`() {
        val done = state.copy(strengthMinutesToday = 30, weekDone = mapOf(DayOfWeek.MONDAY to 30))
        assertEquals(TodayPlan.Done(30, sessions.first { it.day > DayOfWeek.MONDAY }), done.todayPlan())
    }

    @Test fun `planned cardio is one tap to log, until it is logged or recorded`() {
        val tuesday = state.copy(today = DayOfWeek.TUESDAY)
        val run = on(DayOfWeek.TUESDAY)
        assertEquals(TodayPlan.Cardio(run, CardioLog(CardioType.Running, run.minutes)), tuesday.todayPlan())
        val recorded = tuesday.copy(cardioToday = CardioToday(recordedMinutes = 32), weekDone = mapOf(run.day to 32))
        assertEquals(TodayPlan.Done(32, sessions.first { it.day > DayOfWeek.TUESDAY }), recorded.todayPlan())
    }

    @Test fun `mobility, rest and the end of the week`() {
        assertEquals(TodayPlan.Mobility(on(DayOfWeek.WEDNESDAY)), state.copy(today = DayOfWeek.WEDNESDAY).todayPlan())
        val saturday = state.copy(today = DayOfWeek.SATURDAY)
        assertEquals(TodayPlan.Rest(on(DayOfWeek.SUNDAY)), saturday.todayPlan())
        val sunday = state.copy(today = DayOfWeek.SUNDAY, program = state.program.copy(sessions = emptyList()))
        assertEquals(TodayPlan.Rest(null), sunday.todayPlan())
        val other = PlannedSession(DayOfWeek.SUNDAY, ExerciseType.Other, 20)
        val unloggable = sunday.copy(program = state.program.copy(sessions = listOf(other)))
        assertEquals(TodayPlan.Done(0, null), unloggable.todayPlan())
    }
}
