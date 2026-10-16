package se.novila.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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
        // 120 minutes a week of running.
        val history = weekly(ExerciseType.Running, 60) + weekly(ExerciseType.Running, 60, dayOffset = 5)
        val program = TrainingProgramGenerator.generate(history, today)
        assertEquals(130, program.cardioMinutes)
        val cardio = program.sessions.filter { it.type.kind == ExerciseKind.Cardio }
        assertTrue(cardio.all { it.type == ExerciseType.Running })
    }

    @Test fun `from the WHO guideline the cardio is spread over four days`() {
        val history = weekly(ExerciseType.Cycling, 100) + weekly(ExerciseType.Cycling, 100, dayOffset = 5)
        val program = TrainingProgramGenerator.generate(history, today)
        assertEquals(220, program.cardioMinutes)
        assertEquals(4, program.sessions.count { it.type.kind == ExerciseKind.Cardio })
        assertEquals(220, program.minutes(ExerciseKind.Cardio))
    }

    @Test fun `cardio never goes above the upper guideline`() {
        val history = weekly(ExerciseType.Running, 500)
        val program = TrainingProgramGenerator.generate(history, today)
        assertEquals(TrainingProgramGenerator.MAX_CARDIO_MINUTES, program.cardioMinutes)
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
}
