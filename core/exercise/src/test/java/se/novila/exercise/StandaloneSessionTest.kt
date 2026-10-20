package se.novila.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * The watch's workout mode has no program from the phone, so it builds the day's session from the same
 * [StrengthSessions] and runs it with the same [Workout] steps: these tests keep that shared logic honest.
 */
class StandaloneSessionTest {
    private val start = Instant.fromEpochSeconds(2_000)

    @Test fun `the first session runs early in the week and the second from Thursday`() {
        listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY).forEach { day ->
            assertFalse("$day is the first session", StandaloneSession.second(day))
        }
        listOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).forEach { day ->
            assertTrue("$day is the second session", StandaloneSession.second(day))
        }
    }

    @Test fun `the two sessions use different exercises but the same level`() {
        val first = StandaloneSession.of(DayOfWeek.MONDAY, StrengthLevel.Intermediate)
        val second = StandaloneSession.of(DayOfWeek.THURSDAY, StrengthLevel.Intermediate)
        assertEquals(ExerciseType.Strength, first.type)
        assertEquals(StrengthSessions.session(StrengthLevel.Intermediate, second = false), first.exercises)
        assertEquals(StrengthSessions.session(StrengthLevel.Intermediate, second = true), second.exercises)
        assertTrue(first.exercises.map { it.exerciseId } != second.exercises.map { it.exerciseId })
    }

    @Test fun `the level decides the dose and the planned time`() {
        val beginner = StandaloneSession.of(DayOfWeek.MONDAY, StrengthLevel.Beginner)
        val advanced = StandaloneSession.of(DayOfWeek.MONDAY, StrengthLevel.Advanced)
        assertEquals(20, StandaloneSession.minutes(StrengthLevel.Beginner))
        assertEquals(30, StandaloneSession.minutes(StrengthLevel.Intermediate))
        assertEquals(40, StandaloneSession.minutes(StrengthLevel.Advanced))
        assertEquals(StrengthLevel.Beginner.sets, beginner.exercises.first().sets)
        assertEquals(StrengthLevel.Beginner.rest, beginner.exercises.first().rest)
        assertEquals(StrengthLevel.Advanced.sets, advanced.exercises.first().sets)
        assertEquals(StrengthLevel.Advanced.rest, advanced.exercises.first().rest)
    }

    @Test fun `a session starts with the warm-up, rests on the level's timer and ends with the cool-down`() {
        var workout = StandaloneSession.start(DayOfWeek.MONDAY, StrengthLevel.Beginner, start)
        val warmUp = workout.step as WorkoutStep.WarmUp
        assertEquals(Workout.WARM_UP, warmUp.duration)
        assertEquals(179, workout.secondsLeft(start + 1.seconds))

        // The warm-up runs out and the first set waits for the user.
        workout = workout.next(start + Workout.WARM_UP)
        val firstSet = workout.step as WorkoutStep.Work
        assertEquals(1, firstSet.set)
        assertEquals(StrengthLevel.Beginner.rest, (workout.steps[workout.index + 1] as WorkoutStep.Rest).duration)

        // After the set, the rest counts down from the level's rest and is skipped with Next.
        val restAt = start + Workout.WARM_UP + 40.seconds
        workout = workout.next(restAt)
        val rest = workout.step as WorkoutStep.Rest
        assertEquals(StrengthLevel.Beginner.rest, rest.duration)
        assertEquals(StrengthLevel.Beginner.rest.inWholeSeconds.toInt(), workout.secondsLeft(restAt))
        workout = workout.next(restAt + 10.seconds)
        assertEquals(WorkoutStep.Work(rest.next.exercise, rest.next.exerciseNumber, set = 2), workout.step)

        // The cool-down ends the session on its own.
        while (workout.step !is WorkoutStep.CoolDown) {
            workout = workout.next(start + 10_000.seconds)
        }
        assertFalse(workout.isFinished)
        workout = workout.next(start + 20_000.seconds)
        assertTrue(workout.isFinished)
    }
}
