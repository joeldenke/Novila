package se.novila.data

import androidx.datastore.preferences.core.emptyPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import se.novila.coach.CoachPlanner
import se.novila.coach.CoachPreferences
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseKind
import se.novila.exercise.ExerciseType
import se.novila.exercise.TrainingProgramGenerator
import se.novila.model.TrainingToday
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

class TrainingWeekTest {
    private val monday = LocalDate.of(2026, 10, 12)
    private val saved = HealthStore.SavedDays(emptyPreferences())

    @Test fun `without the coach, next week is generated the same way`() {
        val week = trainingWeek(saved, SavedCoach(null), emptyList(), monday)
        assertEquals(monday.plusWeeks(1), week.nextProgram.weekStart)
        assertEquals(
            TrainingProgramGenerator.generate(emptyList(), monday.plusWeeks(1)),
            week.nextProgram,
        )
    }

    @Test fun `with the coach, next week is planned for the Monday after`() {
        val preferences = CoachPreferences(daysPerWeek = 3)
        val week = trainingWeek(saved, SavedCoach(preferences), emptyList(), monday)
        assertEquals(monday.plusWeeks(1), week.nextProgram.weekStart)
        assertEquals(
            CoachPlanner.plan(preferences, emptyList(), monday.plusWeeks(1)).program,
            week.nextProgram,
        )
    }

    @Test fun `this week's program stays the one that counts`() {
        val week = trainingWeek(saved, SavedCoach(null), emptyList(), monday)
        assertEquals(monday, week.program.weekStart)
        assertEquals(week.nextProgram.weekStart, week.program.weekStart.plusWeeks(1))
    }

    @Test fun `today's training is the planned minutes the user can log and what is trained`() {
        val walk = Exercise(ExerciseType.Walking, monday, 25.minutes)
        val week = trainingWeek(saved, SavedCoach(CoachPreferences(daysPerWeek = 5)), listOf(walk), monday)
        val training = week.trainingOn(monday)
        val planned = week.program.sessions
            .filter { it.day == monday.dayOfWeek && it.type.kind != ExerciseKind.Mobility }
            .sumOf { it.minutes }
        assertEquals(TrainingToday(plannedMinutes = planned, doneMinutes = 25), training)
        assertEquals(0, week.trainingOn(monday.plusDays(1)).doneMinutes)
    }
}
