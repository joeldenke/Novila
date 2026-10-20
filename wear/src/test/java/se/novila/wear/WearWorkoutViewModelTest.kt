package se.novila.wear

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import se.novila.exercise.Dosage
import se.novila.exercise.StandaloneSession
import se.novila.exercise.StrengthLevel
import se.novila.exercise.StrengthSessions
import se.novila.exercise.WorkoutStep
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WearWorkoutViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = TestClock()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startScreenShowsTheDaysSessionAtTheChosenLevel() {
        val viewModel = WearWorkoutViewModel(clock)
        val day = LocalDate.now().dayOfWeek

        assertEquals(day, viewModel.state.value.day)
        assertEquals(StandaloneSession.minutes(StrengthLevel.Intermediate), viewModel.state.value.planned.minutes)
        assertEquals(5, viewModel.state.value.planned.exercises.size)

        viewModel.setLevel(StrengthLevel.Beginner)

        assertEquals(4, viewModel.state.value.planned.exercises.size)
        viewModel.setSecond(true)
        assertEquals(StrengthLevel.Beginner, viewModel.state.value.level)
        assertTrue(viewModel.state.value.planned.exercises.first().exerciseId in StrengthSessions.exerciseIds)
    }

    @Test
    fun startBeginsTheWarmUpAndNextMovesThroughTheSets() {
        val viewModel = WearWorkoutViewModel(clock)
        viewModel.start()

        assertTrue(viewModel.state.value.step is WorkoutStep.WarmUp)
        assertEquals(180, viewModel.state.value.secondsLeft)

        viewModel.next()

        val work = viewModel.state.value.step as WorkoutStep.Work
        assertEquals(1, work.exerciseNumber)
        assertEquals(1, work.set)
        assertNull(viewModel.state.value.secondsLeft)

        viewModel.next()

        val rest = viewModel.state.value.step as WorkoutStep.Rest
        assertEquals(StrengthLevel.Intermediate.rest, rest.duration)
        assertEquals(rest.duration.inWholeSeconds.toInt(), viewModel.state.value.secondsLeft)
    }

    @Test
    fun aTimedStepCountsDownAndMovesOnWhenItRunsOut() = runTest(dispatcher) {
        val viewModel = WearWorkoutViewModel(clock)
        viewModel.start()
        clock.instant = clock.instant + 180.seconds

        advanceTimeBy(2_000)

        assertTrue(viewModel.state.value.step is WorkoutStep.Work)
        assertNull(viewModel.state.value.secondsLeft)
    }

    @Test
    fun aRestEndsOnItsOwnAndTheNextSetStarts() = runTest(dispatcher) {
        val viewModel = WearWorkoutViewModel(clock)
        viewModel.start()
        viewModel.next()
        viewModel.next()
        val rest = viewModel.state.value.step as WorkoutStep.Rest
        clock.instant = clock.instant + rest.duration + 1.seconds

        advanceTimeBy(2_000)

        val work = viewModel.state.value.step as WorkoutStep.Work
        assertEquals(1, work.exerciseNumber)
        assertEquals(2, work.set)
    }

    @Test
    fun finishingTheLastStepEndsTheSession() {
        val viewModel = WearWorkoutViewModel(clock)
        viewModel.setLevel(StrengthLevel.Beginner)
        viewModel.start()

        // Warm-up, then every set and rest, then the cool-down.
        repeat(4 * 2 * 2) { viewModel.next() }
        assertTrue(viewModel.state.value.step is WorkoutStep.CoolDown)

        viewModel.next()

        assertTrue(viewModel.state.value.isFinished)
        assertNull(viewModel.state.value.step)
    }

    @Test
    fun resetReturnsToThePlanAndKeepsTheLevel() {
        val viewModel = WearWorkoutViewModel(clock)
        viewModel.setLevel(StrengthLevel.Advanced)
        viewModel.start()

        viewModel.reset()

        val state = viewModel.state.value
        assertNull(state.workout)
        assertEquals(StrengthLevel.Advanced, state.level)
        assertEquals(StandaloneSession.minutes(StrengthLevel.Advanced), state.planned.minutes)
    }

    @Test
    fun thePlanUsesTheDoseTheSharedSessionGives() {
        val viewModel = WearWorkoutViewModel(clock)
        val plank = viewModel.state.value.planned.exercises.first { it.exerciseId == "plank" }

        assertEquals(Dosage.Seconds, plank.dosage)
        assertEquals(StrengthLevel.Intermediate.seconds, plank.amount)
    }

    @Test
    fun theWeekPicksTheSecondSessionOnThursday() {
        assertFalse(StandaloneSession.second(DayOfWeek.MONDAY))
        assertTrue(StandaloneSession.second(DayOfWeek.THURSDAY))
        assertTrue(StandaloneSession.second(DayOfWeek.SUNDAY))
    }

    private class TestClock(var instant: Instant = Instant.fromEpochSeconds(0)) : Clock {
        override fun now(): Instant = instant
    }
}
