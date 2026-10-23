package se.novila.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ProgressionTest {
    private val today = LocalDate.of(2026, 10, 22)
    private val squat =
        PlannedExercise("squat", sets = 3, amount = 10, dosage = Dosage.Repetitions, perSide = false, rest = 60.seconds)
    private val plank =
        PlannedExercise("plank", sets = 2, amount = 30, dosage = Dosage.Seconds, perSide = false, rest = 45.seconds)

    private fun result(id: String, planned: Int, vararg done: Int, daysAgo: Long = 3) =
        ExerciseResult(id, today.minusDays(daysAgo), planned, done.toList())

    private fun log(vararg results: ExerciseResult) = ExerciseLog(results.toList())

    @Test fun `every set reached gives one repetition more, or five seconds for a hold`() {
        assertEquals(11, Progression.dose(squat, log(result("squat", 10, 10, 10, 12)), today).amount)
        assertEquals(35, Progression.dose(plank, log(result("plank", 30, 30, 30)), today).amount)
        assertEquals(10, Progression.dose(squat, log(result("squat", 10, 10, 10, 12)), today).previous)
    }

    @Test fun `it builds on the latest target, one step at a time`() {
        val log = log(result("squat", 10, 10, 10, 10, daysAgo = 10), result("squat", 12, 12, 12, 12))
        assertEquals(13, Progression.dose(squat, log, today).amount)
    }

    @Test fun `a set a little short keeps the target, one clearly short takes a step back`() {
        assertEquals(12, Progression.dose(squat, log(result("squat", 12, 12, 11, 11)), today).amount)
        assertEquals(11, Progression.dose(squat, log(result("squat", 12, 12, 12, 10)), today).amount)
    }

    @Test fun `the progression stays within its steps of the planned dose`() {
        assertEquals(16, Progression.dose(squat, log(result("squat", 20, 20, 20, 20)), today).amount)
        assertEquals(8, Progression.dose(squat, log(result("squat", 6, 2, 2, 2)), today).amount)
        val tiny = squat.copy(amount = 1)
        assertEquals(1, Progression.dose(tiny, log(result("squat", 1, 0, 0, 0)), today).amount)
    }

    @Test fun `without a result in memory the planned dose stays`() {
        assertSame(squat, Progression.dose(squat, log(result("push_up", 10, 10, 10)), today))
        val old = result("squat", 10, 10, 10, 10, daysAgo = Progression.MEMORY_DAYS)
        assertSame(squat, Progression.dose(squat, log(old), today))
        // Today's own session does not move today's target.
        assertSame(squat, Progression.dose(squat, log(result("squat", 10, 10, 10, 10, daysAgo = 0)), today))
    }

    @Test fun `only strength sessions are dosed, rehab is left to pain`() {
        val strength = PlannedSession(DayOfWeek.MONDAY, ExerciseType.Strength, 20, listOf(squat))
        val rehab = PlannedSession(DayOfWeek.WEDNESDAY, ExerciseType.Rehab, 20, listOf(squat))
        val program = TrainingProgram(today, 150, listOf(strength, rehab))
        val dosed = Progression.apply(program, log(result("squat", 10, 10, 10, 10)), today)
        assertEquals(11, dosed.sessions[0].exercises.single().amount)
        assertEquals(10, dosed.sessions[1].exercises.single().amount)
        assertSame(program, Progression.apply(program, ExerciseLog(), today))
    }

    /** The Monday of [today]'s week. */
    private val monday = LocalDate.of(2026, 10, 19)

    /** A squat on the Tuesday of each of the [weeks] weeks before [monday], every set reached. */
    private fun weeksOfSquats(weeks: Int) = (1..weeks).map {
        ExerciseResult("squat", monday.minusWeeks(it.toLong()).plusDays(1), 10, listOf(10, 10, 10))
    }

    /** Last week's Thursday session: [missed] exercises clearly short and [reached] reached. */
    private fun lastWeek(missed: Int, reached: Int) = (0 until missed).map {
        ExerciseResult("missed_$it", monday.minusDays(4), 12, listOf(12, 9, 8))
    } + (0 until reached).map { ExerciseResult("reached_$it", monday.minusDays(4), 10, listOf(10, 10)) }

    @Test fun `four weeks in a row with last week falling short give a lighter week`() {
        // Last week: the squat and one more reached, three clearly short.
        val tired = ExerciseLog((weeksOfSquats(4) + lastWeek(missed = 3, reached = 1)).sortedBy { it.date })
        assertTrue(Progression.lighterWeek(tired, monday))

        // One week without strength in the four: no lighter week.
        val gap = ExerciseLog((weeksOfSquats(4).drop(1).dropLast(1) + lastWeek(2, 0)).sortedBy { it.date })
        assertFalse(Progression.lighterWeek(gap, monday))
        // Last week mostly reached its targets: keep going.
        val strong = ExerciseLog((weeksOfSquats(4) + lastWeek(missed = 2, reached = 3)).sortedBy { it.date })
        assertFalse(Progression.lighterWeek(strong, monday))
        // A single exercise short is not enough.
        assertFalse(Progression.lighterWeek(ExerciseLog(weeksOfSquats(4) + lastWeek(1, 0)), monday))
    }

    @Test fun `a lighter week has a set less and holds the targets`() {
        val log = ExerciseLog((weeksOfSquats(4) + lastWeek(missed = 2, reached = 0)).sortedBy { it.date })
        val program = TrainingProgram(
            monday,
            150,
            listOf(PlannedSession(DayOfWeek.THURSDAY, ExerciseType.Strength, 20, listOf(squat))),
        )
        val lighter = Progression.apply(program, log, monday.plusDays(3))
        assertTrue(lighter.lighterWeek)
        // Every set was reached last time, which would give 11; the lighter week holds 10, with a set less.
        val held = lighter.sessions.single().exercises.single()
        assertEquals(2, held.sets)
        assertEquals(10, held.amount)
        assertEquals(10, held.previous)
        // Without a result for an exercise, the planned target with a set less.
        assertEquals(plank.copy(sets = 1), Progression.held(plank, log, monday.plusDays(3)))
        assertEquals(squat.copy(sets = 1), Progression.held(squat.copy(sets = 1), ExerciseLog(), today))
    }

    @Test fun `an exercise at the same target without reaching it three times has stalled`() {
        val stuck = (3 downTo 1).map { result("squat", 12, 12, 11, 11, daysAgo = it * 3L) }
        assertEquals(setOf("squat"), Progression.stalled(ExerciseLog(stuck), today))

        // Reaching it once, a target that moved, or only two sessions: still moving.
        val reachedOnce = stuck.dropLast(1) + result("squat", 12, 12, 12, 12, daysAgo = 1)
        assertTrue(Progression.stalled(ExerciseLog(reachedOnce), today).isEmpty())
        val moved = listOf(result("squat", 11, 11, 10, daysAgo = 9)) + stuck.drop(1)
        assertTrue(Progression.stalled(ExerciseLog(moved), today).isEmpty())
        assertTrue(Progression.stalled(ExerciseLog(stuck.drop(1)), today).isEmpty())
        // Results older than the memory are forgotten.
        val old = stuck.map { it.copy(date = it.date.minusDays(Progression.MEMORY_DAYS)) }
        assertTrue(Progression.stalled(ExerciseLog(old), today).isEmpty())
    }

    @Test fun `a workout remembers the repetitions said and the seconds held`() {
        val start = Instant.fromEpochSeconds(1_000)
        val session = PlannedSession(DayOfWeek.MONDAY, ExerciseType.Strength, 20, listOf(squat.copy(sets = 2), plank))
        var workout = Workout.start(session, start).next(start) // past the warm-up
        assertEquals(10, workout.doneInSet)
        workout = workout.withDone(8)
        assertEquals(8, workout.doneInSet)
        workout = workout.next(start).next(start) // set done, rest skipped
        workout = workout.next(start).next(start) // second set as planned, rest skipped
        // The first hold ends after 20 of its 30 seconds; the second runs out.
        workout = workout.next(start + 20.seconds).next(start + 20.seconds)
        workout = workout.next(start + 60.seconds)
        assertNull(workout.doneInSet)
        assertEquals(
            listOf(
                ExerciseResult("squat", today, 10, listOf(8, 10)),
                ExerciseResult("plank", today, 30, listOf(20, 30)),
            ),
            workout.results(today),
        )
        // The squat missed a set by two and steps back; the plank was held 20 of 30 seconds once, and steps back.
        assertEquals(listOf(-1, -5), workout.nextChanges().map { it.second })
        val reached = Workout.start(session, start).next(start).next(start)
        assertEquals(listOf(1), reached.nextChanges().map { it.second })
    }

    @Test fun `the log keeps one result per exercise and day and forgets old ones`() {
        val old = result("squat", 8, 8, 8, daysAgo = Progression.MEMORY_DAYS + 1)
        val earlier = result("squat", 10, 9, 9, daysAgo = 0)
        val log = ExerciseLog(listOf(old, earlier)).withSession(listOf(result("squat", 10, 10, 10, daysAgo = 0)))
        assertEquals(listOf(result("squat", 10, 10, 10, daysAgo = 0)), log.results)
        assertSame(log, log.withSession(emptyList()))
    }

    @Test fun `the log survives encoding, and unreadable lines are skipped`() {
        val log = log(result("squat", 10, 10, 9, 8, daysAgo = 5), result("plank", 30, 30, 25))
        assertEquals(log, ExerciseLogCodec.decode(ExerciseLogCodec.encode(log)))
        assertEquals(ExerciseLog(), ExerciseLogCodec.decode(null))
        assertEquals(ExerciseLog(), ExerciseLogCodec.decode("x1\n2026-10-20|squat|10|10"))
        assertEquals(
            listOf(result("squat", 10, 10, daysAgo = 2)),
            ExerciseLogCodec.decode("p1\n2026-10-20|squat|10|10\nbroken\n2026-10-20||10|10\n2026-10-20|a|0|1").results,
        )
    }
}
