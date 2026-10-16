package se.novila.exercise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.novila.anatomy.ExerciseLibrary
import se.novila.anatomy.MuscleGroup
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.minutes

class StrengthSessionsTest {
    private val today = LocalDate.of(2026, 10, 15)

    private fun week(level: StrengthLevel) = listOf(
        StrengthSessions.session(level, second = false),
        StrengthSessions.session(level, second = true),
    )

    @Test fun `every exercise exists in the anatomy library`() {
        StrengthSessions.exerciseIds.forEach { assertNotNull(it, ExerciseLibrary.byId(it)) }
    }

    @Test fun `the large muscle groups are trained on both strength days`() {
        val large = setOf(MuscleGroup.Quads, MuscleGroup.Glutes, MuscleGroup.Chest, MuscleGroup.Abs)
        StrengthLevel.entries.forEach { level ->
            week(level).forEach { session ->
                val trained = session.flatMap { ExerciseLibrary.byId(it.exerciseId)!!.primary }.toSet()
                assertTrue("$level misses ${large - trained}", trained.containsAll(large))
            }
        }
    }

    @Test fun `more experience means more exercises, more work and shorter rests`() {
        val sizes = StrengthLevel.entries.map { StrengthSessions.session(it, second = false).size }
        assertEquals(listOf(4, 5, 6), sizes)
        val beginner = StrengthSessions.session(StrengthLevel.Beginner, second = false).first()
        val advanced = StrengthSessions.session(StrengthLevel.Advanced, second = false).first()
        assertTrue(advanced.sets >= beginner.sets && advanced.amount > beginner.amount)
        assertTrue(advanced.rest < beginner.rest)
    }

    @Test fun `holds are dosed in seconds and one-sided exercises per side`() {
        val a = StrengthSessions.session(StrengthLevel.Advanced, second = false).associateBy { it.exerciseId }
        val b = StrengthSessions.session(StrengthLevel.Advanced, second = true).associateBy { it.exerciseId }
        assertEquals(Dosage.Seconds, a.getValue("plank").dosage)
        assertEquals(StrengthLevel.Advanced.seconds, a.getValue("plank").amount)
        assertTrue(b.getValue("reverse_lunge").perSide)
        assertEquals(Dosage.Repetitions, a.getValue("squat").dosage)
    }

    @Test fun `the second session varies the exercises`() {
        StrengthLevel.entries.forEach { level ->
            val (a, b) = week(level).map { session -> session.map { it.exerciseId }.toSet() }
            assertTrue(level.name, (a - b).isNotEmpty())
        }
    }

    @Test fun `the generator puts the sessions on strength days with rest in between`() {
        val program = TrainingProgramGenerator.generate(emptyList(), today)
        val strength = program.sessions.filter { it.type == ExerciseType.Strength }
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), strength.map { it.day })
        assertEquals(week(StrengthLevel.Beginner), strength.map { it.exercises })
        val days = strength.map { program.weekStart.plusDays(it.day.value - 1L) }
        assertTrue(ChronoUnit.HOURS.between(days[0].atStartOfDay(), days[1].atStartOfDay()) >= 48)
        assertTrue(program.sessions.filter { it.type != ExerciseType.Strength }.all { it.exercises.isEmpty() })
    }

    @Test fun `regular strength training raises the level`() {
        val monday = LocalDate.of(2026, 10, 12)
        val twiceAWeek = (1..4L).flatMap { week ->
            listOf(1L, 4L).map {
                Exercise(ExerciseType.Strength, monday.minusWeeks(week).plusDays(it), 30.minutes)
            }
        }
        val program = TrainingProgramGenerator.generate(twiceAWeek, today)
        val monday1 = program.sessions.first { it.type == ExerciseType.Strength }
        assertEquals(StrengthSessions.session(StrengthLevel.Advanced, second = false), monday1.exercises)
    }
}
