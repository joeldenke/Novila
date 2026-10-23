package se.novila.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.novila.anatomy.Equipment
import se.novila.anatomy.ExerciseLibrary
import se.novila.anatomy.MuscleGroup
import se.novila.exercise.Dosage
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseKind
import se.novila.exercise.ExerciseType
import se.novila.exercise.PlannedSession
import se.novila.exercise.StrengthLevel
import se.novila.exercise.TrainingProgram
import se.novila.exercise.TrainingProgramGenerator
import se.novila.recovery.RecoveryStatus
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class CoachPlannerTest {
    /** A Thursday. The program's week starts on Monday the 12th. */
    private val today = LocalDate.of(2026, 10, 15)
    private val monday = LocalDate.of(2026, 10, 12)

    private fun plan(
        preferences: CoachPreferences = CoachPreferences(),
        history: List<Exercise> = emptyList(),
        recovery: RecoveryStatus = RecoveryStatus.Unknown,
        age: Int? = null,
        stalled: Set<String> = emptySet(),
    ) = CoachPlanner.plan(preferences, history, today, recovery = recovery, age = age, stalled = stalled)

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

    @Test fun `there are two strength days with a rest day between them whenever the week has room`() {
        everyAnswer.forEach { preferences ->
            val strength = plan(preferences).program.strength.map { it.day }
            assertTrue(preferences.toString(), strength.size >= minOf(2, preferences.daysPerWeek))
            if (strength.size >= 2) assertTrue(preferences.toString(), CoachPlanner.minGap(strength) >= 2)
        }
    }

    @Test fun `one day a week is a full strength session, and the coach says what WHO advises`() {
        CoachGoal.entries.filter { it != CoachGoal.Rehab }.forEach { goal ->
            val plan = plan(CoachPreferences(goal, daysPerWeek = 1))
            assertEquals(goal.toString(), 1, plan.program.sessions.size)
            assertEquals(goal.toString(), 1, plan.program.strength.size)
            assertTrue(goal.toString(), CoachNote.OneStrengthDay in plan.notes)
            assertFalse(goal.toString(), CoachNote.RestBetweenStrengthDays in plan.notes)
        }
    }

    @Test fun `the user's own days are the days of the week`() {
        val days = setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        val preferences = CoachPreferences().withDays(3).copy(days = days)
        val program = plan(preferences).program
        assertEquals(days.sorted(), program.sessions.map { it.day })
        // The strength days are the two furthest apart: Tuesday and Saturday.
        assertEquals(listOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY), program.strength.map { it.day })
    }

    @Test fun `another suggestion changes the exercises, and the next one changes them again`() {
        listOf(CoachPreferences(), CoachPreferences(equipment = setOf(Equipment.Dumbbells, Equipment.Band))).forEach {
            val first = plan(it).program.strength.flatMap { session -> session.ids() }
            val second = plan(it.nextVariation()).program.strength.flatMap { session -> session.ids() }
            val third = plan(it.nextVariation().nextVariation()).program.strength.flatMap { session -> session.ids() }
            assertNotEquals(it.toString(), first, second)
            assertNotEquals(it.toString(), second, third)
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

    @Test fun `rehab-only exercises stay out of ordinary sessions`() {
        val rehabOnly = CoachCatalog.exercises.filter { it.rehabOnly }.map { it.id }
        everyAnswer.forEach { preferences ->
            plan(preferences).program.strength.flatMap { it.ids() }.forEach { id ->
                assertTrue("$preferences: $id", id !in rehabOnly)
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

    @Test fun `without equipment every session is bodyweight`() {
        everyAnswer.forEach { preferences ->
            plan(preferences, strengthTwiceAWeek()).program.strength.flatMap { it.ids() }.forEach { id ->
                assertEquals("$preferences: $id", null, CoachCatalog.byId(id)!!.equipment)
            }
        }
    }

    @Test fun `the coach uses only the equipment the user has, and prefers it to bodyweight`() {
        Equipment.entries.forEach { equipment ->
            val preferences = CoachPreferences(CoachGoal.Strength, 4, 45, equipment = setOf(equipment))
            val plan = plan(preferences, strengthTwiceAWeek())
            val used = plan.program.strength.flatMap { it.ids() }
            used.forEach { id ->
                val needs = CoachCatalog.byId(id)!!.equipment
                assertTrue("$equipment: $id", needs == null || needs == equipment)
            }
            assertTrue("$equipment: $used", used.any { CoachCatalog.byId(it)!!.equipment == equipment })
            assertTrue(plan.notes.contains(CoachNote.UsesEquipment(setOf(equipment))))
        }
    }

    @Test fun `equipment never takes the place of a balanced base`() {
        val all = Equipment.entries.toSet()
        (CoachGoal.entries - CoachGoal.Rehab).forEach { goal ->
            plan(CoachPreferences(goal, 4, 30, equipment = all), strengthTwiceAWeek()).program.strength.forEach {
                val patterns = it.ids().map { id -> CoachCatalog.byId(id)!!.pattern }
                val lower = patterns.any { p -> p == MovementPattern.Legs || p == MovementPattern.Hinge }
                assertTrue("$goal: ${it.ids()}", lower)
                assertTrue("$goal: ${it.ids()}", MovementPattern.Push in patterns)
            }
        }
    }

    @Test fun `barbell exercises wait until the user already trains strength`() {
        val beginner = plan(CoachPreferences(equipment = setOf(Equipment.Barbell))).program.strength
            .flatMap { it.ids() }
        assertTrue(beginner.none { CoachCatalog.byId(it)!!.equipment == Equipment.Barbell })
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
        everyAnswer.filter { it.considerations.isEmpty() && it.daysPerWeek >= 2 }.forEach { preferences ->
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

    @Test fun `a stalled exercise makes way for another for the same movement, and the coach says so`() {
        fun ids(plan: CoachPlan) = plan.program.sessions.flatMap { it.exercises }.map { it.exerciseId }.toSet()
        val usual = plan()
        val stuck = ids(usual).first { id -> CoachCatalog.byId(id)!!.pattern == MovementPattern.Legs }
        val swapped = plan(stalled = setOf(stuck))
        assertFalse(stuck in ids(swapped))
        // Still a lower-body exercise in every strength session.
        swapped.program.sessions.filter { it.type == ExerciseType.Strength }.forEach { session ->
            assertTrue(session.exercises.any { CoachCatalog.byId(it.exerciseId)!!.pattern in LOWER_BODY })
        }
        assertTrue(CoachNote.Stalled(listOf(stuck)) in swapped.notes)
        assertTrue(usual.notes.none { it is CoachNote.Stalled })
    }

    @Test fun `from 65 there are three strength days, each starting with a balance exercise`() {
        fun strength(plan: CoachPlan) = plan.program.sessions.filter { it.type == ExerciseType.Strength }
        fun CoachPlan.balanceExercises() = strength(this).flatMap { it.exercises }
            .filter { CoachCatalog.byId(it.exerciseId)!!.pattern == MovementPattern.Balance }

        val older = plan(CoachPreferences(daysPerWeek = 4), age = CoachPlanner.BALANCE_AGE)
        assertEquals(3, strength(older).size)
        assertTrue(
            strength(older).all {
                CoachCatalog.byId(it.exercises.first().exerciseId)!!.pattern == MovementPattern.Balance
            },
        )
        assertTrue(CoachNote.Balance in older.notes)
        // A beginner stands on one leg; the hinge on one leg waits until strength training is a habit.
        assertTrue(older.balanceExercises().all { it.exerciseId == "single_leg_stand" })

        // Never more strength days than training days.
        assertEquals(2, strength(plan(CoachPreferences(daysPerWeek = 2), age = 80)).size)

        for (younger in listOf(plan(CoachPreferences(daysPerWeek = 4), age = CoachPlanner.BALANCE_AGE - 1), plan())) {
            assertEquals(2, strength(younger).size)
            assertTrue(younger.balanceExercises().isEmpty())
            assertFalse(CoachNote.Balance in younger.notes)
        }
    }

    @Test fun `running counts double towards WHO's guideline, the same minutes of walking don't`() {
        fun weekly(type: ExerciseType) = (1..4L).map { Exercise(type, monday.minusWeeks(it).plusDays(1), 75.minutes) }
        val preferences = CoachPreferences(CoachGoal.Endurance, daysPerWeek = 5, sessionMinutes = 60)

        // 75 minutes of running count as 150; a tenth more is 165, or three runs of 30 minutes.
        val runner = plan(preferences, weekly(ExerciseType.Running))
        assertEquals(90, runner.program.cardioMinutes)
        assertTrue(CoachNote.VigorousCardio(90, 180) in runner.notes)
        assertTrue(runner.notes.none { it is CoachNote.CardioBelowGuideline })

        val walker = plan(preferences, weekly(ExerciseType.Walking))
        assertEquals(90, walker.program.cardioMinutes)
        assertTrue(walker.notes.none { it is CoachNote.VigorousCardio })
        assertTrue(CoachNote.CardioBelowGuideline(90) in walker.notes)
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
                assertEquals(minOf(3, days), sessions.size)
                if (sessions.size >= 2) assertTrue("$area $days", CoachPlanner.minGap(sessions.map { it.day }) >= 2)
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

    @Test fun `low recovery gives today's strength session one set less and explains why`() {
        val plain = plan()
        val low = plan(recovery = RecoveryStatus.Low)
        val before = plain.program.sessions.first { it.day == today.dayOfWeek }
        val after = low.program.sessions.first { it.day == today.dayOfWeek }
        assertEquals(ExerciseType.Strength, after.type)
        assertEquals(before.ids(), after.ids())
        after.exercises.forEach { exercise ->
            val original = before.exercises.first { it.exerciseId == exercise.exerciseId }
            assertEquals(exercise.exerciseId, maxOf(1, original.sets - 1), exercise.sets)
        }
        assertTrue(low.notes.contains(CoachNote.Recovery))
        assertTrue(!plain.notes.contains(CoachNote.Recovery))
        assertEquals(
            plain.program.sessions.filter { it.day != today.dayOfWeek },
            low.program.sessions.filter { it.day != today.dayOfWeek },
        )
    }

    @Test fun `a lighter session the user chose is lighter the same way, with its own note`() {
        val low = plan(recovery = RecoveryStatus.Low)
        val lighter = CoachPlanner.plan(CoachPreferences(), emptyList(), today, lighter = true)
        assertEquals(low.program, lighter.program)
        assertTrue(lighter.notes.contains(CoachNote.Lighter))
        assertTrue(lighter.notes.none { it == CoachNote.Recovery })
        // When the watch says so too, the recovery note says why.
        val both = CoachPlanner.plan(
            CoachPreferences(),
            emptyList(),
            today,
            recovery = RecoveryStatus.Low,
            lighter = true,
        )
        assertEquals(low.program, both.program)
        assertEquals(low.notes, both.notes)
    }

    @Test fun `low recovery lightens today's cardio`() {
        val preferences = CoachPreferences(CoachGoal.Health, daysPerWeek = 5)
        val tuesday = monday.plusDays(1)
        val plain = CoachPlanner.plan(preferences, emptyList(), tuesday)
        val low = CoachPlanner.plan(preferences, emptyList(), tuesday, recovery = RecoveryStatus.Low)
        val before = plain.program.sessions.first { it.day == DayOfWeek.TUESDAY }
        val after = low.program.sessions.first { it.day == DayOfWeek.TUESDAY }
        assertEquals(ExerciseKind.Cardio, after.type.kind)
        assertTrue(after.minutes < before.minutes)
        assertEquals(
            TrainingProgramGenerator.roundTo5(before.minutes * 0.8).coerceAtLeast(10),
            after.minutes,
        )
    }

    @Test fun `normal or unknown recovery changes nothing`() {
        val plain = plan()
        assertEquals(plain, plan(recovery = RecoveryStatus.Normal))
        assertEquals(plain, plan(recovery = RecoveryStatus.Unknown))
    }

    @Test fun `a low recovery on a day without a session adds no note`() {
        val wednesday = monday.plusDays(2)
        val low = CoachPlanner.plan(CoachPreferences(), emptyList(), wednesday, recovery = RecoveryStatus.Low)
        assertTrue(low.notes.none { it == CoachNote.Recovery })
    }

    @Test fun `rehab is left alone when recovery is low`() {
        val plain = rehab(RehabArea.Knee, phase = 2)
        val low = CoachPlanner.plan(
            CoachPreferences(CoachGoal.Rehab, daysPerWeek = 4, rehabArea = RehabArea.Knee, rehabPhase = 2),
            strengthTwiceAWeek(),
            today,
            recovery = RecoveryStatus.Low,
        )
        assertEquals(plain.program, low.program)
        assertTrue(low.notes.none { it == CoachNote.Recovery })
    }
}

private val LOWER_BODY = setOf(MovementPattern.Legs, MovementPattern.Hinge)
