package se.novila.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.novila.exercise.ExerciseType
import java.time.LocalDate

class CoachFeedbackTest {
    private val today = LocalDate.of(2026, 10, 15)

    private fun session(daysAgo: Long, effort: Int? = null, pain: Int? = null, phase: Int = 0) =
        SessionFeedback(today.minusDays(daysAgo), effort, pain, phase)

    @Test fun `no feedback changes nothing`() {
        assertNull(CoachFeedback.effort(emptyList(), today))
        assertNull(CoachFeedback.rehab(emptyList(), phase = 1, today))
        assertNull(CoachFeedback.effort(listOf(session(1, effort = 9)), today))
    }

    @Test fun `two very hard sessions take a set away`() {
        val check = CoachFeedback.effort(listOf(session(1, effort = 9), session(4, effort = 10)), today)
        assertEquals(EffortCheck(EffortAdvice.Easier, effort = 10, pain = null), check)
    }

    @Test fun `two easy sessions add repetitions, and only the latest two count`() {
        val feedback = listOf(session(1, effort = 3), session(3, effort = 4), session(6, effort = 10))
        assertEquals(EffortAdvice.Harder, CoachFeedback.effort(feedback, today)?.advice)
        assertEquals(EffortAdvice.Same, CoachFeedback.effort(listOf(session(1, 6), session(3, 7)), today)?.advice)
    }

    @Test fun `pain after the latest session eases the dose`() {
        val check = CoachFeedback.effort(listOf(session(1, effort = 3, pain = 7), session(3, effort = 3)), today)
        assertEquals(EffortCheck(EffortAdvice.Easier, effort = null, pain = 7), check)
    }

    @Test fun `old and rehab sessions don't count for the dose`() {
        val old = listOf(session(20, effort = 10), session(25, effort = 10))
        assertNull(CoachFeedback.effort(old, today))
        val rehab = listOf(session(1, effort = 10, phase = 1), session(3, effort = 10, phase = 1))
        assertNull(CoachFeedback.effort(rehab, today))
    }

    @Test fun `three calm rehab sessions suggest the next phase`() {
        val feedback = listOf(session(1, 5, 2, 1), session(3, 4, 1, 1), session(5, null, 3, 1))
        assertEquals(
            RehabCheck(RehabAdvice.MoveOn, phase = 2, calmSessions = 3, pain = 2),
            CoachFeedback.rehab(feedback, 1, today),
        )
        assertEquals(RehabAdvice.Done, CoachFeedback.rehab(feedback.map { it.copy(rehabPhase = 3) }, 3, today)?.advice)
    }

    @Test fun `fewer calm sessions keep going`() {
        val feedback = listOf(session(1, pain = 1, phase = 1), session(3, pain = 5, phase = 1))
        assertEquals(
            RehabCheck(RehabAdvice.KeepGoing, phase = 1, calmSessions = 1, pain = 1),
            CoachFeedback.rehab(feedback, 1, today),
        )
    }

    @Test fun `too much pain steps back, or sends to care in the first phase`() {
        assertEquals(RehabAdvice.Stay, CoachFeedback.rehab(listOf(session(1, pain = 4, phase = 2)), 2, today)?.advice)
        val painful = CoachFeedback.rehab(listOf(session(1, pain = 6, phase = 2)), 2, today)
        assertEquals(RehabCheck(RehabAdvice.StepBack, phase = 1, calmSessions = 0, pain = 6), painful)
        val first = CoachFeedback.rehab(listOf(session(1, pain = 8, phase = 1)), 1, today)
        assertEquals(RehabAdvice.SeekCare, first?.advice)
    }

    @Test fun `only sessions since the phase last changed count`() {
        // Three calm sessions in phase 1, then a painful one in phase 2 and a step back: phase 1 starts over.
        val feedback = listOf(
            session(2, pain = 7, phase = 2),
            session(4, pain = 1, phase = 1),
            session(6, pain = 1, phase = 1),
            session(8, pain = 1, phase = 1),
        )
        assertNull(CoachFeedback.rehab(feedback, 1, today))
        assertEquals(RehabAdvice.StepBack, CoachFeedback.rehab(feedback, 2, today)?.advice)
    }

    @Test fun `the plan follows the feedback`() {
        val preferences = CoachPreferences()
        val usual = CoachPlanner.plan(preferences, emptyList(), today)
        val hard = CoachPlanner.plan(preferences, emptyList(), today, listOf(session(1, 9), session(3, 10)))
        val easy = CoachPlanner.plan(preferences, emptyList(), today, listOf(session(1, 3), session(3, 3)))
        fun exercises(plan: CoachPlan) = plan.program.sessions.flatMap { it.exercises }
        fun sets(plan: CoachPlan) = exercises(plan).associate { it.exerciseId to it.sets }
        fun amounts(plan: CoachPlan) = exercises(plan).associate { it.exerciseId to it.amount }
        sets(hard).forEach { (id, sets) -> sets(usual)[id]?.let { assertEquals(id, maxOf(1, it - 1), sets) } }
        amounts(easy).forEach { (id, amount) -> amounts(usual)[id]?.let { assertTrue(id, amount > it) } }
        assertTrue(hard.notes.contains(CoachNote.Effort(EffortCheck(EffortAdvice.Easier, 10, null))))
        assertTrue(usual.notes.none { it is CoachNote.Effort })
    }

    @Test fun `a rehab plan suggests the phase but keeps the user's`() {
        val preferences = CoachPreferences(goal = CoachGoal.Rehab, rehabPhase = 1)
        val calm = listOf(1L, 3L, 5L).map { session(it, pain = 1, phase = 1) }
        val plan = CoachPlanner.plan(preferences, emptyList(), today, calm)
        assertEquals(2, plan.suggestedPhase)
        assertEquals(CoachPlanner.plan(preferences, emptyList(), today).program, plan.program)
        assertTrue(plan.notes.any { it is CoachNote.Rehab && it.check.advice == RehabAdvice.MoveOn })
        assertTrue(plan.program.sessions.any { it.type == ExerciseType.Rehab })
        assertNull(CoachPlanner.plan(preferences, emptyList(), today).suggestedPhase)
    }
}
