package se.novila.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class WorkoutTest {
    private val squat =
        PlannedExercise("squat", sets = 2, amount = 10, dosage = Dosage.Repetitions, perSide = false, rest = 60.seconds)
    private val plank =
        PlannedExercise("plank", sets = 2, amount = 30, dosage = Dosage.Seconds, perSide = false, rest = 45.seconds)
    private val session = PlannedSession(DayOfWeek.MONDAY, ExerciseType.Strength, 20, listOf(squat, plank))
    private val start = Instant.fromEpochSeconds(1_000)

    @Test fun `every set is followed by a rest except the last`() {
        val steps = Workout.steps(listOf(squat, plank))
        assertEquals(7, steps.size)
        assertEquals(listOf(true, false, true, false, true, false, true), steps.map { it is WorkoutStep.Work })
        val restBeforePlank = steps[3] as WorkoutStep.Rest
        assertEquals(60.seconds, restBeforePlank.duration)
        assertEquals(WorkoutStep.Work(plank, exerciseNumber = 2, set = 1), restBeforePlank.next)
    }

    @Test fun `repetition sets wait for the user, rests and holds run on a timer`() {
        var workout = Workout.start(session, start)
        assertNull("squat set waits", workout.endsAt)
        workout = workout.next(start + 40.seconds)
        assertTrue(workout.step is WorkoutStep.Rest)
        assertEquals(60, workout.secondsLeft(start + 40.seconds))
        assertEquals(1, workout.secondsLeft(start + 99_500.milliseconds))
        assertEquals(0, workout.secondsLeft(start + 200.seconds))
        workout = workout.next(start + 100.seconds).next(start + 140.seconds).next(start + 200.seconds)
        assertEquals(WorkoutStep.Work(plank, exerciseNumber = 2, set = 1), workout.step)
        assertEquals(30, workout.secondsLeft(start + 200.seconds))
    }

    @Test fun `the session finishes after the last set`() {
        var workout = Workout.start(session, start)
        repeat(6) { workout = workout.next(start + it.minutes) }
        assertFalse(workout.isFinished)
        assertEquals(3, workout.setsDone)
        workout = workout.next(start + 20.minutes)
        assertTrue(workout.isFinished)
        assertEquals(4, workout.setsDone)
        assertEquals(1f, workout.progress)
        assertEquals("time stops at the end", 20, workout.minutes(start + 99.minutes))
        assertEquals("next after the end changes nothing", workout, workout.next(start + 30.minutes))
    }

    @Test fun `a session takes at least a minute`() {
        assertEquals(1, Workout.start(session, start).minutes(start + 5.seconds))
    }

    @Test fun `generated sessions fit roughly in their planned minutes`() {
        StrengthLevel.entries.forEach { level ->
            val exercises = StrengthSessions.session(level, second = false)
            val time = Workout.steps(exercises).fold(Duration.ZERO) { sum, step ->
                sum + when (step) {
                    is WorkoutStep.Rest -> step.duration

                    // About four seconds a repetition.
                    is WorkoutStep.Work ->
                        step.exercise.amount.seconds * if (step.exercise.dosage == Dosage.Seconds) 1 else 4
                }
            }
            assertTrue("$level takes $time", time.inWholeMinutes in 10..45)
        }
    }
}
