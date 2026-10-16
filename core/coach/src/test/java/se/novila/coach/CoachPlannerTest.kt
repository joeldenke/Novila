package se.novila.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.novila.anatomy.ExerciseLibrary
import se.novila.anatomy.MuscleGroup
import se.novila.exercise.Dosage
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseKind
import se.novila.exercise.ExerciseType
import se.novila.exercise.PlannedSession
import se.novila.exercise.StrengthLevel
import se.novila.exercise.TrainingProgram
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CoachPlannerTest {
    /** A Thursday. The program's week starts on Monday the 12th. */
    private val today = LocalDate.of(2026, 10, 15)
    private val monday = LocalDate.of(2026, 10, 12)

    private fun plan(preferences: CoachPreferences = CoachPreferences(), history: List<Exercise> = emptyList()) =
        CoachPlanner.plan(preferences, history, today)

    private val TrainingProgram.strength: List<PlannedSession>
        get() = sessions.filter { it.type == ExerciseType.Strength }

    private fun PlannedSession.ids() = exercises.map { it.exerciseId }

    private fun PlannedSession.muscles(): Set<MuscleGroup> =
        exercises.flatMap { ExerciseLibrary.byId(it.exerciseId)!!.primary }.toSet()

    /** Every combination of answers for a training goal, apart from the variation. Rehab is tested on its own. */
    private val everyAnswer: List<CoachPreferences> = (CoachGoal.entries - CoachGoal.Rehab).flatMap { goal ->
        (CoachPreferences.MIN_DAYS..CoachPreferences.MAX_DAYS).flatMap { days ->
            listOf(15, 30, 60).flatMap { minutes ->
                listOf(emptySet(), Consideration.entries.toSet(), setOf(Consideration.Knees)).map { considered ->
                    CoachPreferences(goal, days, minutes, considerations = considered)
                }
            }
        }
    }

    @Test fun `every exercise in the library is known to the coach and every entry exists`() {
        ExerciseLibrary.all.forEach { assertNotNull(it.id, CoachCatalog.byId(it.id)) }
        CoachCatalog.exercises.forEach { assertNotNull(it.id, ExerciseLibrary.byId(it.id)) }
    }

    @Test fun `the week has as many training days as asked, at most one session a day`() {
        everyAnswer.forEach { preferences ->
            val days = plan(preferences).program.sessions.map { it.day }
            assertEquals(preferences.toString(), preferences.daysPerWeek, days.size)
            assertEquals(preferences.toString(), days.distinct(), days)
            assertEquals(days.sorted(), days)
        }
    }

    @Test fun `there are always at least two strength days with a rest day between them`() {
        everyAnswer.forEach { preferences ->
            val strength = plan(preferences).program.strength.map { it.day }
            assertTrue(preferences.toString(), strength.size >= 2)
            assertTrue(preferences.toString(), CoachPlanner.minGap(strength) >= 2)
        }
    }

    @Test fun `strength as the goal gives a third strength day when there is time`() {
        assertEquals(2, plan(CoachPreferences(CoachGoal.Strength, daysPerWeek = 3)).program.strength.size)
        assertEquals(3, plan(CoachPreferences(CoachGoal.Strength, daysPerWeek = 4)).program.strength.size)
        assertEquals(
            listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            plan(CoachPreferences(CoachGoal.Strength, daysPerWeek = 6)).program.strength.map { it.day },
        )
    }

    @Test fun `every session trains the legs, a push and the trunk`() {
        val trunk = setOf(MuscleGroup.Abs, MuscleGroup.LowerBack)
        everyAnswer.forEach { preferences ->
            plan(preferences).program.strength.forEach { session ->
                val muscles = session.muscles()
                assertTrue("$preferences: ${session.ids()}", MuscleGroup.Glutes in muscles)
                assertTrue("$preferences: ${session.ids()}", MuscleGroup.Chest in muscles)
                assertTrue("$preferences: ${session.ids()}", muscles.any { it in trunk })
                assertTrue(session.exercises.size in 3..7)
            }
        }
    }

    @Test fun `considerations leave out the exercises that don't suit them`() {
        Consideration.entries.forEach { consideration ->
            val used = (CoachGoal.entries - CoachGoal.Rehab).flatMap { goal ->
                val history = strengthTwiceAWeek()
                plan(CoachPreferences(goal, 6, 60, considerations = setOf(consideration)), history)
                    .program.strength.flatMap { it.ids() }
            }
            used.forEach { id ->
                assertTrue("$consideration: $id", CoachCatalog.byId(id)!!.suits(setOf(consideration)))
            }
        }
        val knees = plan(CoachPreferences(considerations = setOf(Consideration.Knees)), strengthTwiceAWeek())
        assertTrue(knees.program.strength.flatMap { it.ids() }.none { it == "jumping_jack" || it == "reverse_lunge" })
        assertTrue(knees.notes.contains(CoachNote.Considered(Consideration.Knees)))
    }

    @Test fun `a beginner gets no exercises above their level`() {
        everyAnswer.forEach { preferences ->
            plan(preferences).program.strength.flatMap { it.ids() }.forEach { id ->
                assertEquals("$preferences: $id", StrengthLevel.Beginner, CoachCatalog.byId(id)!!.level)
            }
        }
    }

    @Test fun `longer sessions get more exercises, and the estimate stays near the time asked`() {
        val history = strengthTwiceAWeek()
        val short = plan(CoachPreferences(sessionMinutes = 15), history).program.strength.first()
        val long = plan(CoachPreferences(sessionMinutes = 45), history).program.strength.first()
        assertTrue("${short.ids()} vs ${long.ids()}", long.exercises.size > short.exercises.size)
        assertTrue(long.minutes in 30..50)
    }

    @Test fun `the two sessions of a week vary`() {
        everyAnswer.filter { it.considerations.isEmpty() }.forEach { preferences ->
            val (a, b) = plan(preferences, strengthTwiceAWeek()).program.strength.map { it.ids().toSet() }
            assertNotEquals(preferences.toString(), a, b)
        }
    }

    @Test fun `a focus area gets more of its muscles trained`() {
        fun legSets(preferences: CoachPreferences) = plan(preferences, strengthTwiceAWeek()).program.strength
            .flatMap { it.exercises }
            .filter { ExerciseLibrary.byId(it.exerciseId)!!.primary.any(BodyArea.Legs.muscles::contains) }
            .sumOf { it.sets }
        listOf(15, 30, 45).forEach { minutes ->
            val plain = CoachPreferences(sessionMinutes = minutes)
            assertTrue("$minutes", legSets(plain.copy(focus = setOf(BodyArea.Legs))) > legSets(plain))
        }
    }

    @Test fun `the goal sets the dosage`() {
        val squat = CoachCatalog.byId("squat")!!
        val level = StrengthLevel.Intermediate
        val health = CoachPlanner.dose(squat, level, CoachGoal.Health)
        val strength = CoachPlanner.dose(squat, level, CoachGoal.Strength)
        val endurance = CoachPlanner.dose(squat, level, CoachGoal.Endurance)
        assertEquals(level.sets, health.sets)
        assertTrue(strength.sets > health.sets && strength.rest > health.rest)
        assertTrue(endurance.amount > health.amount && endurance.rest < health.rest)
    }

    @Test fun `endurance gets more cardio days and mobility gets mobility days`() {
        fun count(goal: CoachGoal, kind: ExerciseKind) =
            plan(CoachPreferences(goal, daysPerWeek = 5)).program.sessions.count { it.type.kind == kind }
        assertTrue(count(CoachGoal.Endurance, ExerciseKind.Cardio) > count(CoachGoal.Health, ExerciseKind.Cardio))
        assertEquals(2, count(CoachGoal.Mobility, ExerciseKind.Mobility))
    }

    @Test fun `cardio builds on recent workouts and never outgrows the session time`() {
        val running = (1..4L).map {
            Exercise(ExerciseType.Running, monday.minusWeeks(it).plusDays(1), 200.minutes)
        }
        val program = plan(CoachPreferences(CoachGoal.Endurance, daysPerWeek = 6, sessionMinutes = 60), running).program
        val cardio = program.sessions.filter { it.type.kind == ExerciseKind.Cardio }
        assertTrue(cardio.all { it.type == ExerciseType.Running && it.minutes <= 60 })
        assertTrue(program.cardioMinutes <= 220)

        val newcomer = plan(CoachPreferences(daysPerWeek = 3, sessionMinutes = 30))
        assertEquals(30, newcomer.program.cardioMinutes)
        assertTrue(newcomer.notes.contains(CoachNote.CardioBelowGuideline(30)))
    }

    @Test fun `the same answers give the same program, a new variation another one`() {
        val preferences = CoachPreferences(sessionMinutes = 45)
        val history = strengthTwiceAWeek()
        assertEquals(plan(preferences, history), plan(preferences, history))
        val variations = (0..5).map { plan(preferences.copy(variation = it), history).program }.toSet()
        assertTrue(variations.size > 1)
    }

    @Test fun `days are spread as evenly as possible`() {
        val week = DayOfWeek.entries.take(6)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), CoachPlanner.spreadOut(week, 2))
        assertEquals(3, CoachPlanner.minGap(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)))
        assertEquals(2, CoachPlanner.minGap(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY)))
    }

    @Test fun `the notes explain the goal, the level and the rest days`() {
        val notes = plan(CoachPreferences(CoachGoal.Strength, focus = setOf(BodyArea.Core))).notes
        assertEquals(CoachNote.Goal(CoachGoal.Strength), notes.first())
        assertTrue(notes.contains(CoachNote.Level(StrengthLevel.Beginner)))
        assertTrue(notes.contains(CoachNote.RestBetweenStrengthDays))
        assertTrue(notes.contains(CoachNote.Focus(setOf(BodyArea.Core))))
    }

    private fun rehab(area: RehabArea, phase: Int, days: Int = 4, considered: Set<Consideration> = emptySet()) =
        plan(
            CoachPreferences(
                CoachGoal.Rehab,
                daysPerWeek = days,
                considerations = considered,
                rehabArea = area,
                rehabPhase = phase,
            ),
            strengthTwiceAWeek(),
        )

    @Test fun `a rehab week has three calm sessions every other day and walks in between`() {
        RehabArea.entries.forEach { area ->
            (CoachPreferences.MIN_DAYS..CoachPreferences.MAX_DAYS).forEach { days ->
                val program = rehab(area, phase = 1, days = days).program
                val sessions = program.sessions.filter { it.type == ExerciseType.Rehab }
                assertEquals(3, sessions.size)
                assertTrue("$area $days", CoachPlanner.minGap(sessions.map { it.day }) >= 2)
                val others = program.sessions - sessions.toSet()
                assertTrue(others.all { it.type == ExerciseType.Walking && it.exercises.isEmpty() })
                assertEquals(days, program.sessions.size)
            }
        }
    }

    @Test fun `rehab uses the phase's exercises, gets harder with each phase and ignores the strength level`() {
        RehabArea.entries.forEach { area ->
            val phases = (1..3).map { rehab(area, it).program.sessions.first { s -> s.type == ExerciseType.Rehab } }
            phases.forEachIndexed { i, session ->
                assertEquals(CoachPlanner.rehabExercises(area, i + 1), session.ids())
                assertTrue(session.exercises.all { it.rest == 60.seconds })
            }
            assertTrue(phases[0].exercises.size <= phases[2].exercises.size)
            val first = phases[0].exercises.first { it.dosage == Dosage.Repetitions }
            val last = phases[2].exercises.first { it.exerciseId == first.exerciseId }
            assertTrue(last.sets > first.sets && last.amount > first.amount)
        }
        // Rehab never starts with jumps, lunges or anything above a beginner's level.
        RehabArea.entries.flatMap { area -> (1..3).flatMap { CoachPlanner.rehabExercises(area, it) } }.forEach {
            val exercise = CoachCatalog.byId(it)!!
            assertTrue(it, !exercise.jump && exercise.level == StrengthLevel.Beginner)
        }
    }

    @Test fun `rehab respects the considerations and explains the pain limit`() {
        val knee = rehab(RehabArea.Knee, 3, considered = setOf(Consideration.Knees))
        assertTrue(knee.program.sessions.flatMap { it.ids() }.none { it == "split_squat" })
        val back = rehab(RehabArea.LowerBack, 3, considered = setOf(Consideration.Back))
        assertTrue(back.program.sessions.flatMap { it.ids() }.none { it == "good_morning" })
        assertEquals(
            listOf(CoachNote.Goal(CoachGoal.Rehab), CoachNote.RehabPhase(RehabArea.LowerBack, 3), CoachNote.PainGuide),
            back.notes.take(3),
        )
    }

    private fun strengthTwiceAWeek() = (1..4L).flatMap { week ->
        listOf(1L, 4L).map {
            Exercise(ExerciseType.Strength, monday.minusWeeks(week).plusDays(it), 30.minutes)
        }
    }
}
