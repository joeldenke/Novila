package se.novila.data

import androidx.datastore.preferences.core.emptyPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import se.novila.coach.CoachPlanner
import se.novila.coach.CoachPreferences
import se.novila.exercise.TrainingProgramGenerator
import java.time.LocalDate

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
}
