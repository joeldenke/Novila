package se.novila.coach

import org.junit.Assert.assertEquals
import org.junit.Test
import se.novila.exercise.TrainingLoad
import se.novila.model.DayEntry
import se.novila.model.FoodLog
import se.novila.model.Goals
import se.novila.model.TrainingToday
import se.novila.model.glassesOfWater
import se.novila.recovery.RecoveryStatus
import se.novila.sleep.SleepCoachPreferences
import java.time.LocalDate
import java.time.LocalTime

class DailyCoachTest {
    private val date = LocalDate.of(2026, 10, 18)
    private val goals = Goals()

    /** A day with only the sleep logged, so the coach still has something to say about training and food. */
    private fun day(sleep: Float = 7.5f, water: Int = 0, food: FoodLog = FoodLog()) =
        DayEntry(date, sleepHours = sleep, drinks = glassesOfWater(water), food = food)

    /** A day whose food and water are all at their goals, so nutrition asks for nothing. */
    private val fullDay = day(
        sleep = 7.5f,
        water = goals.waterGlasses,
        food = FoodLog(vegetables = 5, meals = 3, protein = 3, wholeGrain = 3),
    )

    @Test fun `a short night lightens the session and points to an earlier bedtime`() {
        val coach = dailyCoach(
            day = day(sleep = 5.5f),
            goals = goals,
            training = TrainingToday(plannedMinutes = 30),
            bedtimeGoal = LocalTime.of(22, 30),
        )

        assertEquals(CoachFocus.Recover, coach.focus)
        assertEquals(
            listOf(
                CoachAdvice.ShortNight(5.5f),
                CoachAdvice.LighterSession(30),
                CoachAdvice.EarlierBedtime(LocalTime.of(22, 30)),
            ),
            coach.advice,
        )
    }

    @Test fun `a short night on a rest day says so and still points to bedtime`() {
        val coach = dailyCoach(day = day(sleep = 6f), goals = goals, training = TrainingToday())

        assertEquals(
            listOf(
                CoachAdvice.ShortNight(6f),
                CoachAdvice.RestDay,
                CoachAdvice.EarlierBedtime(SleepCoachPreferences.DEFAULT_BEDTIME),
            ),
            coach.advice,
        )
    }

    @Test fun `a low recovery lightens the session even after a good night`() {
        val coach = dailyCoach(
            day = fullDay,
            training = TrainingToday(plannedMinutes = 30),
            recovery = RecoveryStatus.Low,
        )

        assertEquals(CoachFocus.Recover, coach.focus)
        assertEquals(listOf(CoachAdvice.LowRecovery, CoachAdvice.LighterSession(30)), coach.advice)
    }

    @Test fun `only the remaining minutes of the session count`() {
        val coach = dailyCoach(
            day = day(sleep = 5f),
            training = TrainingToday(plannedMinutes = 30, doneMinutes = 20),
        )

        assertEquals(CoachAdvice.LighterSession(10), coach.advice[1])
    }

    @Test fun `a full day with a session planned points to the session`() {
        val coach = dailyCoach(day = fullDay, training = TrainingToday(plannedMinutes = 30))

        assertEquals(CoachFocus.Train, coach.focus)
        assertEquals(listOf(CoachAdvice.PlannedSession(30)), coach.advice)
    }

    @Test fun `training and a food step are said in one breath`() {
        val coach = dailyCoach(day = day(sleep = 7.5f), training = TrainingToday(plannedMinutes = 30))

        assertEquals(CoachFocus.Train, coach.focus)
        assertEquals(2, coach.advice.size)
        assertEquals(CoachAdvice.PlannedSession(30), coach.advice[0])
        assertEquals(true, coach.advice[1] is CoachAdvice.AddPortion)
    }

    @Test fun `a done session leaves the next food step`() {
        val coach = dailyCoach(day = day(), training = TrainingToday(plannedMinutes = 30, doneMinutes = 30))

        assertEquals(CoachFocus.Nutrition, coach.focus)
        assertEquals(true, coach.main is CoachAdvice.AddPortion)
    }

    @Test fun `a complete day is simply well done`() {
        val coach = dailyCoach(day = fullDay, training = TrainingToday(plannedMinutes = 30, doneMinutes = 30))

        assertEquals(CoachFocus.Balance, coach.focus)
        assertEquals(listOf(CoachAdvice.WellDone), coach.advice)
    }

    @Test fun `a night that was not logged is not called short`() {
        val coach = dailyCoach(
            day = fullDay.copy(sleepHours = 0f),
            training = TrainingToday(plannedMinutes = 30),
            bedtimeGoal = LocalTime.of(21, 0),
        )

        assertEquals(CoachFocus.Train, coach.focus)
        assertEquals(listOf(CoachAdvice.PlannedSession(30)), coach.advice)
    }

    @Test fun `a calm day says its reasons at once, then lightens the session`() {
        val calm = readiness(day(sleep = 5.5f), goals, RecoveryStatus.Low)!!
        val coach = dailyCoach(
            day = day(sleep = 5.5f),
            goals = goals,
            training = TrainingToday(plannedMinutes = 30),
            recovery = RecoveryStatus.Low,
            readiness = calm,
        )

        assertEquals(CoachFocus.Recover, coach.focus)
        assertEquals(
            listOf(
                CoachAdvice.CalmDay(calm.reasons),
                CoachAdvice.LighterSession(30),
                CoachAdvice.EarlierBedtime(SleepCoachPreferences.DEFAULT_BEDTIME),
            ),
            coach.advice,
        )
    }

    @Test fun `a calm day from the training load and symptoms is gentler even after a good night`() {
        val heavy = TrainingLoad(week = 1400, usualWeek = 1000, monotony = 1.0, sessions = 4, rated = 2)
        val calm = readiness(fullDay, goals, load = heavy, cycleSymptoms = true)!!
        val coach = dailyCoach(day = fullDay, training = TrainingToday(plannedMinutes = 30), readiness = calm)

        assertEquals(CoachFocus.Recover, coach.focus)
        assertEquals(listOf(CoachAdvice.CalmDay(calm.reasons), CoachAdvice.LighterSession(30)), coach.advice)
    }

    @Test fun `a ready body is said with its reasons before the planned session`() {
        val ready = readiness(fullDay, goals, RecoveryStatus.Normal)!!
        val coach = dailyCoach(
            day = fullDay,
            training = TrainingToday(plannedMinutes = 30),
            recovery = RecoveryStatus.Normal,
            readiness = ready,
        )

        assertEquals(CoachFocus.Train, coach.focus)
        assertEquals(listOf(CoachAdvice.ReadyDay(ready.reasons), CoachAdvice.PlannedSession(30)), coach.advice)
    }
}
