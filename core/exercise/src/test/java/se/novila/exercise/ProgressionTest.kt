package se.novila.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
