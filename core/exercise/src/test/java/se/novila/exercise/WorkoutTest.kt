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

    @Test fun `the session warms up first, rests between sets and cools down last`() {
        val steps = Workout.steps(listOf(squat, plank))
        assertEquals(9, steps.size)
        val warmUp = steps.first() as WorkoutStep.WarmUp
        assertEquals(Workout.WARM_UP, warmUp.duration)
        assertEquals(WorkoutStep.Work(squat, exerciseNumber = 1, set = 1), warmUp.next)
        val everyOtherSet = listOf(true, false, true, false, true, false, true)
        assertEquals(everyOtherSet, steps.drop(1).take(7).map { it is WorkoutStep.Work })
        val restBeforePlank = steps[4] as WorkoutStep.Rest
        assertEquals(60.seconds, restBeforePlank.duration)
        assertEquals(WorkoutStep.Work(plank, exerciseNumber = 2, set = 1), restBeforePlank.next)
        assertEquals(WorkoutStep.CoolDown(Workout.COOL_DOWN), steps.last())
    }

    @Test fun `warm-up and cool-down are timed, and together five minutes like the coach budgets`() {
        val workout = Workout.start(session, start)
        assertTrue(workout.step is WorkoutStep.WarmUp)
        assertEquals(170, workout.secondsLeft(start + 10.seconds))
        assertEquals(5.minutes, Workout.WARM_UP + Workout.COOL_DOWN)
    }

    @Test fun `repetition sets wait for the user, timed steps run on a timer`() {
        var workout = Workout.start(session, start)
        val warmUp = workout.step as WorkoutStep.WarmUp
        assertEquals(Workout.WARM_UP, warmUp.duration)
        assertEquals(170, workout.secondsLeft(start + 10.seconds))
        assertEquals(1, workout.secondsLeft(start + warmUp.duration - 1.seconds))
        workout = workout.next(start + warmUp.duration)
        assertEquals(WorkoutStep.Work(squat, exerciseNumber = 1, set = 1), workout.step)
        assertNull("squat set waits", workout.endsAt)
        workout = workout.next(start + warmUp.duration + 40.seconds)
        assertTrue(workout.step is WorkoutStep.Rest)
        assertEquals(60, workout.secondsLeft(start + warmUp.duration + 40.seconds))
        assertEquals(1, workout.secondsLeft(start + warmUp.duration + 99_500.milliseconds))
        assertEquals(0, workout.secondsLeft(start + warmUp.duration + 200.seconds))
        workout = workout
            .next(start + warmUp.duration + 100.seconds)
            .next(start + warmUp.duration + 140.seconds)
            .next(start + warmUp.duration + 200.seconds)
        assertEquals(WorkoutStep.Work(plank, exerciseNumber = 2, set = 1), workout.step)
        assertEquals(30, workout.secondsLeft(start + warmUp.duration + 200.seconds))
    }

    @Test fun `the session finishes after the cool-down`() {
        var workout = Workout.start(session, start)
        assertFalse(workout.isFinished)
        repeat(8) { workout = workout.next(start + it.minutes) }
        assertFalse("the cool-down still runs", workout.isFinished)
        assertEquals(WorkoutStep.CoolDown(Workout.COOL_DOWN), workout.step)
        assertEquals(4, workout.setsDone)
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
                    is WorkoutStep.Timed -> step.duration

                    // About four seconds a repetition.
                    is WorkoutStep.Work ->
                        step.exercise.amount.seconds * if (step.exercise.dosage == Dosage.Seconds) 1 else 4
                }
            }
            assertTrue("$level takes $time", time.inWholeMinutes in 10..45)
        }
    }
}
