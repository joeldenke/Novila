package se.novila.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class WorkoutTest {
    private val squat =
        PlannedExercise("squat", sets = 2, amount = 10, dosage = Dosage.Repetitions, perSide = false, restSeconds = 60)
    private val plank =
        PlannedExercise("plank", sets = 2, amount = 30, dosage = Dosage.Seconds, perSide = false, restSeconds = 45)
    private val session = PlannedSession(DayOfWeek.MONDAY, ExerciseType.Strength, 20, listOf(squat, plank))
    private val start = 1_000_000L

    @Test fun `every set is followed by a rest except the last`() {
        val steps = Workout.steps(listOf(squat, plank))
        assertEquals(7, steps.size)
        assertEquals(listOf(true, false, true, false, true, false, true), steps.map { it is WorkoutStep.Work })
        val restBeforePlank = steps[3] as WorkoutStep.Rest
        assertEquals(60, restBeforePlank.seconds)
        assertEquals(WorkoutStep.Work(plank, exerciseNumber = 2, set = 1), restBeforePlank.next)
    }

    @Test fun `repetition sets wait for the user, rests and holds run on a timer`() {
        var workout = Workout.start(session, start)
        assertNull("squat set waits", workout.endsAtMillis)
        workout = workout.next(start + 40_000)
        assertTrue(workout.step is WorkoutStep.Rest)
        assertEquals(60, workout.secondsLeft(start + 40_000))
        assertEquals(1, workout.secondsLeft(start + 99_500))
        assertEquals(0, workout.secondsLeft(start + 200_000))
        workout = workout.next(start + 100_000).next(start + 140_000).next(start + 200_000)
        assertEquals(WorkoutStep.Work(plank, exerciseNumber = 2, set = 1), workout.step)
        assertEquals(30, workout.secondsLeft(start + 200_000))
    }

    @Test fun `the session finishes after the last set`() {
        var workout = Workout.start(session, start)
        repeat(6) { workout = workout.next(start + it * 60_000L) }
        assertFalse(workout.isFinished)
        assertEquals(3, workout.setsDone)
        workout = workout.next(start + 20 * 60_000L)
        assertTrue(workout.isFinished)
        assertEquals(4, workout.setsDone)
        assertEquals(1f, workout.progress)
        assertEquals("time stops at the end", 20, workout.minutes(start + 99 * 60_000L))
        assertEquals("next after the end changes nothing", workout, workout.next(start + 30 * 60_000L))
    }

    @Test fun `a session takes at least a minute`() {
        assertEquals(1, Workout.start(session, start).minutes(start + 5_000))
    }

    @Test fun `generated sessions fit roughly in their planned minutes`() {
        StrengthLevel.entries.forEach { level ->
            val exercises = StrengthSessions.session(level, second = false)
            val seconds = Workout.steps(exercises).sumOf { step ->
                when (step) {
                    is WorkoutStep.Rest -> step.seconds

                    // About four seconds a repetition.
                    is WorkoutStep.Work -> step.exercise.amount * if (step.exercise.dosage == Dosage.Seconds) 1 else 4
                }
            }
            assertTrue("$level takes ${seconds / 60} min", seconds / 60 in 10..45)
        }
    }
}
