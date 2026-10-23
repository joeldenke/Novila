package se.novila.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.novila.recovery.RecoveryStatus
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

class TrainingProgramGeneratorTest {
    /** A Thursday. The program's week starts on Monday the 12th. */
    private val today = LocalDate.of(2026, 10, 15)
    private val monday = LocalDate.of(2026, 10, 12)

    private fun weekly(type: ExerciseType, minutes: Long, weeks: Int = 4, dayOffset: Long = 1) = (1..weeks).map {
        Exercise(type, monday.minusWeeks(it.toLong()).plusDays(dayOffset), minutes.minutes)
    }

    private fun TrainingProgram.minutes(kind: ExerciseKind) =
        sessions.filter { it.type.kind == kind }.sumOf { it.minutes }

    @Test fun `a new user gets a gentle program of walks, strength and mobility`() {
        val program = TrainingProgramGenerator.generate(emptyList(), today)
        assertEquals(monday, program.weekStart)
        assertEquals(TrainingProgramGenerator.MIN_CARDIO_MINUTES, program.cardioMinutes)
        assertEquals(
            listOf(
                PlannedSession(DayOfWeek.MONDAY, ExerciseType.Strength, 20),
                PlannedSession(DayOfWeek.TUESDAY, ExerciseType.Walking, 30),
                PlannedSession(DayOfWeek.WEDNESDAY, ExerciseType.Mobility, 15),
                PlannedSession(DayOfWeek.THURSDAY, ExerciseType.Strength, 20),
                PlannedSession(DayOfWeek.FRIDAY, ExerciseType.Walking, 30),
                PlannedSession(DayOfWeek.SUNDAY, ExerciseType.Walking, 30),
            ),
            program.sessions.map { it.copy(exercises = emptyList()) },
        )
    }

    @Test fun `cardio rises by at most a tenth over the recent weekly average`() {
        // 120 minutes a week of cycling.
        val history = weekly(ExerciseType.Cycling, 60) + weekly(ExerciseType.Cycling, 60, dayOffset = 5)
        val program = TrainingProgramGenerator.generate(history, today)
        assertEquals(130, program.cardioMinutes)
        val cardio = program.sessions.filter { it.type.kind == ExerciseKind.Cardio }
        assertTrue(cardio.all { it.type == ExerciseType.Cycling })
    }

    @Test fun `a running minute counts as two, as WHO counts vigorous cardio`() {
        assertEquals(60, Exercise(ExerciseType.Running, monday, 30.minutes).moderateMinutes)
        assertEquals(30, Exercise(ExerciseType.Walking, monday, 30.minutes).moderateMinutes)
        assertEquals(30, Exercise(ExerciseType.Cycling, monday, 30.minutes).moderateMinutes)

        // 80 minutes a week of running count as 160, above the guideline: a tenth more is 175, or about 88 minutes of
        // running, spread over four days.
        val running = weekly(ExerciseType.Running, 40) + weekly(ExerciseType.Running, 40, dayOffset = 5)
        val program = TrainingProgramGenerator.generate(running, today)
        assertEquals(175, TrainingProgramGenerator.summarize(running, monday).cardioTarget)
        assertEquals(90, program.cardioMinutes)
        val runs = program.sessions.filter { it.type.kind == ExerciseKind.Cardio }
        assertEquals(List(4) { 20 }, runs.map { it.minutes })
        assertTrue(runs.all { it.type == ExerciseType.Running })

        // The same minutes of walking are below the guideline: three walks.
        val walking = weekly(ExerciseType.Walking, 40) + weekly(ExerciseType.Walking, 40, dayOffset = 5)
        val walks = TrainingProgramGenerator.generate(walking, today).sessions
            .filter { it.type == ExerciseType.Walking }
        assertEquals(List(3) { 30 }, walks.map { it.minutes })
    }

    @Test fun `from the WHO guideline the cardio is spread over four days`() {
        val history = weekly(ExerciseType.Cycling, 100) + weekly(ExerciseType.Cycling, 100, dayOffset = 5)
        val program = TrainingProgramGenerator.generate(history, today)
        assertEquals(220, program.cardioMinutes)
        assertEquals(4, program.sessions.count { it.type.kind == ExerciseKind.Cardio })
        assertEquals(220, program.minutes(ExerciseKind.Cardio))
    }

    @Test fun `cardio never goes above the upper guideline`() {
        val history = weekly(ExerciseType.Cycling, 500)
        val program = TrainingProgramGenerator.generate(history, today)
        assertEquals(TrainingProgramGenerator.MAX_CARDIO_MINUTES, program.cardioMinutes)
        // 300 moderate minutes are 150 of running, WHO's upper guideline for vigorous cardio.
        val running = TrainingProgramGenerator.generate(weekly(ExerciseType.Running, 500), today)
        assertEquals(TrainingProgramGenerator.MAX_CARDIO_MINUTES / 2, running.cardioMinutes)
    }

    @Test fun `strength sessions grow with the strength habit`() {
        val once = weekly(ExerciseType.Strength, 30)
        val twice = once + weekly(ExerciseType.Strength, 30, dayOffset = 3)
        assertEquals(60, TrainingProgramGenerator.generate(once, today).minutes(ExerciseKind.Strength))
        assertEquals(80, TrainingProgramGenerator.generate(twice, today).minutes(ExerciseKind.Strength))
    }

    @Test fun `this week's and older workouts don't count`() {
        val thisWeek = Exercise(ExerciseType.Running, monday, 600.minutes)
        val tooOld = Exercise(ExerciseType.Running, monday.minusWeeks(5), 600.minutes)
        val program = TrainingProgramGenerator.generate(listOf(thisWeek, tooOld), today)
        assertEquals(TrainingProgramGenerator.MIN_CARDIO_MINUTES, program.cardioMinutes)
        assertTrue(program.sessions.none { it.type == ExerciseType.Running })
    }

    @Test fun `low recovery shortens today's strength session and leaves the other days alone`() {
        val program = TrainingProgramGenerator.generate(emptyList(), today, RecoveryStatus.Low)
        assertEquals(
            listOf(DayOfWeek.MONDAY to 20, DayOfWeek.THURSDAY to 15),
            program.sessions.filter { it.type.kind == ExerciseKind.Strength }.map { it.day to it.minutes },
        )
        assertEquals(15, program.sessions.first { it.day == DayOfWeek.WEDNESDAY }.minutes)
    }

    @Test fun `low recovery takes a set off each of today's exercises, so the session is as short as it says`() {
        fun thursday(program: TrainingProgram) = program.sessions.first { it.day == DayOfWeek.THURSDAY }
        val plain = thursday(TrainingProgramGenerator.generate(emptyList(), today))
        val low = thursday(TrainingProgramGenerator.generate(emptyList(), today, RecoveryStatus.Low))
        assertEquals(plain.exercises.map { it.exerciseId }, low.exercises.map { it.exerciseId })
        assertEquals(plain.exercises.map { (it.sets - 1).coerceAtLeast(1) }, low.exercises.map { it.sets })
    }

    @Test fun `low recovery lightens today's cardio and leaves the other days alone`() {
        val tuesday = monday.plusDays(1)
        val program = TrainingProgramGenerator.generate(emptyList(), tuesday, RecoveryStatus.Low)
        assertEquals(
            listOf(DayOfWeek.TUESDAY to 25, DayOfWeek.FRIDAY to 30, DayOfWeek.SUNDAY to 30),
            program.sessions.filter { it.type.kind == ExerciseKind.Cardio }.map { it.day to it.minutes },
        )
    }

    @Test fun `unknown or normal recovery changes nothing`() {
        val plain = TrainingProgramGenerator.generate(emptyList(), today)
        assertEquals(plain, TrainingProgramGenerator.generate(emptyList(), today, RecoveryStatus.Normal))
        assertEquals(plain, TrainingProgramGenerator.generate(emptyList(), today, RecoveryStatus.Unknown))
    }
}
