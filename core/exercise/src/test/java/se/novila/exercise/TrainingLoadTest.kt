package se.novila.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

class TrainingLoadTest {
    private val today = LocalDate.of(2026, 10, 22)

    private fun strength(daysAgo: Long, minutes: Int = 30) =
        Exercise(ExerciseType.Strength, today.minusDays(daysAgo), minutes.minutes)

    private fun walk(daysAgo: Long, minutes: Int = 30) =
        Exercise(ExerciseType.Walking, today.minusDays(daysAgo), minutes.minutes)

    /** The same week as the current one ([days] ago in it), [weeks] times over, before it. */
    private fun earlierWeeks(weeks: Int, days: List<Long>, make: (Long) -> Exercise) =
        (1..weeks).flatMap { week -> days.map { make(it + 7L * week) } }

    @Test fun `nothing trained this week has no load`() {
        assertNull(trainingLoad(listOf(strength(daysAgo = 10)), emptyMap(), today))
    }

    @Test fun `a session counts its minutes times its effort, the user's own when given`() {
        val history = listOf(strength(daysAgo = 1), walk(daysAgo = 2, minutes = 40))
        val load = trainingLoad(history, mapOf(today.minusDays(1) to 8), today)!!
        // 30 min at RPE 8, and a walk with the moderate effort cardio is assumed to have.
        assertEquals(30 * 8 + 40 * assumedEffort(ExerciseKind.Cardio), load.week)
        assertEquals(2, load.sessions)
        assertEquals(1, load.rated)
    }

    @Test fun `the effort after a strength session doesn't rate a walk the same day`() {
        val load = trainingLoad(listOf(walk(daysAgo = 0)), mapOf(today to 9), today)!!
        assertEquals(30 * assumedEffort(ExerciseKind.Cardio), load.week)
        assertEquals(0, load.rated)
    }

    @Test fun `the usual week is unknown until the history reaches three weeks back`() {
        val history = listOf(strength(daysAgo = 1)) + earlierWeeks(weeks = 1, days = listOf(1L), make = ::strength)
        val load = trainingLoad(history, emptyMap(), today)!!
        assertNull(load.usualWeek)
        assertEquals(LoadComparison.Unknown, load.comparison)
    }

    @Test fun `a week like the three before is usual`() {
        val days = listOf(1L, 3L, 5L)
        val history = days.map(::strength) + earlierWeeks(weeks = 3, days = days, make = ::strength)
        val load = trainingLoad(history, emptyMap(), today)!!
        assertEquals(load.week, load.usualWeek)
        assertEquals(LoadComparison.Usual, load.comparison)
    }

    @Test fun `a much harder week is heavier than usual, and an easier one lighter`() {
        val usual = earlierWeeks(weeks = 3, days = listOf(1L, 3L, 5L), make = ::strength)
        val harder = listOf(1L, 2L, 3L, 5L).map { strength(it, minutes = 45) } + usual
        assertEquals(LoadComparison.Heavier, trainingLoad(harder, emptyMap(), today)!!.comparison)
        val easier = listOf(strength(daysAgo = 2)) + usual
        assertEquals(LoadComparison.Lighter, trainingLoad(easier, emptyMap(), today)!!.comparison)
    }

    @Test fun `the same session every day is uniform, and rest days between are not`() {
        val everyDay = (0L..6L).map(::strength)
        val uniform = trainingLoad(everyDay, emptyMap(), today)!!
        assertTrue(uniform.uniform)
        assertTrue(uniform.strain!! > uniform.week)
        val withRest = listOf(0L, 2L, 4L).map(::strength)
        assertFalse(trainingLoad(withRest, emptyMap(), today)!!.uniform)
    }

    @Test fun `monotony needs a few sessions`() {
        val load = trainingLoad(listOf(strength(0), strength(1)), emptyMap(), today)!!
        assertNull(load.monotony)
        assertNull(load.strain)
        assertFalse(load.uniform)
    }
}
