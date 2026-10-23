package se.novila.data

import androidx.datastore.preferences.core.emptyPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import se.novila.coach.CoachPlanner
import se.novila.coach.CoachPreferences
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseKind
import se.novila.exercise.ExerciseLog
import se.novila.exercise.ExerciseResult
import se.novila.exercise.ExerciseType
import se.novila.exercise.Progression
import se.novila.exercise.TrainingProgramGenerator
import se.novila.model.CardioLog
import se.novila.model.CardioType
import se.novila.model.DayEntry
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

    @Test fun `a day with only mobility is no training day for the reminder`() {
        val week = trainingWeek(saved, SavedCoach(null), emptyList(), monday)
        val wednesday = monday.plusDays(2)
        assertEquals(
            listOf(ExerciseType.Mobility),
            week.program.sessions.filter { it.day == wednesday.dayOfWeek }.map { it.type },
        )
        assertFalse(week.hasSession(wednesday))
        assertTrue(week.hasSession(monday))
    }

    @Test fun `the strength sessions build on what the user did last time`() {
        val plain = trainingWeek(saved, SavedCoach(null), emptyList(), monday)
        val first = plain.program.sessions.first { it.exercises.isNotEmpty() }.exercises.first()
        val sets = List(first.sets) { first.amount }
        val reached = ExerciseResult(first.exerciseId, monday.minusDays(4), first.amount, sets)
        val week = trainingWeek(saved, SavedCoach(null), emptyList(), monday, progress = ExerciseLog(listOf(reached)))
        val dosed = week.program.sessions.first { it.exercises.isNotEmpty() }.exercises.first()
        assertEquals(first.amount + Progression.step(first.dosage), dosed.amount)
        assertEquals(first.amount, dosed.previous)
    }

    @Test fun `minutes per date count Novila's own sessions with the watch's, the same walk once`() {
        val days = listOf(
            DayEntry(monday, strengthMinutes = 30).withCardio(CardioLog(CardioType.Walking, 20)),
            DayEntry(monday.plusDays(1)),
        )
        val recorded = listOf(
            // The watch's recording of the same walk, a little longer, and a run the app never saw.
            Exercise(ExerciseType.Walking, monday, 25.minutes),
            Exercise(ExerciseType.Running, monday.plusDays(2), 40.minutes),
        )
        assertEquals(
            mapOf(monday to 55, monday.plusDays(2) to 40),
            trainingMinutesByDate(days, recorded),
        )
    }
}
