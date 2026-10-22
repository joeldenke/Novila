package se.novila.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.SleepFactor
import java.time.LocalDate
import java.time.LocalTime

class SleepCoachTest {
    private val monday = LocalDate.of(2026, 10, 12)
    private val goal = SleepCoachPreferences(
        bedtime = LocalTime.of(22, 30),
        wakeTime = LocalTime.of(6, 30),
    )

    /** A night in bed from 22:30 to 6:30, asleep for [hours], logged on [daysAgo] days ago. */
    private fun night(daysAgo: Long, hours: Float = 8f, factors: Set<SleepFactor> = emptySet()) = DayEntry(
        date = monday.minusDays(daysAgo),
        bedtime = goal.bedtime,
        wakeTime = goal.wakeTime,
        sleepHours = hours,
        sleepQuality = 3,
        sleepFactors = factors,
    )

    private fun plan(days: List<DayEntry>, preferences: SleepCoachPreferences = goal, now: LocalTime = LocalTime.NOON) =
        sleepCoachPlan(days, preferences, Goals(), now)

    @Test fun `the routine puts each step at its time before bedtime`() {
        val preferences = goal.copy(windDown = setOf(WindDownStep.ScreensOff, WindDownStep.PrepareTomorrow))
        assertEquals(
            listOf(
                WindDownMoment(WindDownStep.PrepareTomorrow, LocalTime.of(20, 30)),
                WindDownMoment(WindDownStep.ScreensOff, LocalTime.of(21, 30)),
            ),
            plan(emptyList(), preferences).routine,
        )
    }

    @Test fun `tonight is the next step still ahead, and gone once the steps are past`() {
        val preferences = goal.copy(windDown = setOf(WindDownStep.ScreensOff, WindDownStep.Read))
        assertEquals(WindDownStep.ScreensOff, plan(emptyList(), preferences, LocalTime.of(21, 0)).tonight?.step)
        assertEquals(WindDownStep.Read, plan(emptyList(), preferences, LocalTime.of(21, 40)).tonight?.step)
        assertNull(plan(emptyList(), preferences, LocalTime.of(23, 0)).tonight)
    }

    @Test fun `the week counts the nights within half an hour of the bedtime goal`() {
        val days = listOf(
            night(6).copy(bedtime = LocalTime.of(22, 15)),
            night(5).copy(bedtime = LocalTime.of(23, 30)),
            night(4).copy(bedtime = null),
        )
        assertEquals(SleepCoachWeek(nearGoal = 1, logged = 2), plan(days).week)
    }

    @Test fun `scattered bedtimes suggest keeping the bedtime goal`() {
        val days = (0 until 7).map { i ->
            night(i.toLong()).copy(bedtime = if (i % 2 == 0) LocalTime.of(0, 30) else goal.bedtime)
        }
        assertTrue(SleepCoachTip.RegularBedtime in plan(days).tips)
    }

    @Test fun `steady bedtimes near the goal give no regularity tip`() {
        val days = (0 until 7).map { night(it.toLong()) }
        assertTrue(SleepCoachTip.RegularBedtime !in plan(days).tips)
    }

    @Test fun `wake times far from the goal suggest keeping the wake time steady`() {
        val days = (0 until 7).map { night(it.toLong(), hours = 6f).copy(wakeTime = LocalTime.of(9, 0)) }
        assertTrue(SleepCoachTip.FixedWakeTime in plan(days).tips)
    }

    @Test fun `too little of the bed time asleep suggests getting out of bed`() {
        // In bed 22:30–06:30 (8 h) but asleep for five: plenty of time awake in bed.
        val days = (0 until 7).map { night(it.toLong(), hours = 5f) }
        assertTrue(SleepCoachTip.OutOfBed in plan(days).tips)
    }

    @Test fun `a factor logged before three nights gets its tip, the most often one first`() {
        val days = (0 until 6).map {
            night(it.toLong(), factors = if (it < 4) setOf(SleepFactor.LateCaffeine) else setOf(SleepFactor.Screens))
        }.plus(night(7))
        assertEquals(SleepCoachTip.LateCaffeine, plan(days).tips.first())
    }

    @Test fun `a period is never a tip, however often it is marked`() {
        val days = (0 until 6).map {
            night(it.toLong(), factors = if (it < 2) setOf(SleepFactor.Screens) else setOf(SleepFactor.Period))
        }.plus(night(7))
        assertEquals(emptyList<SleepCoachTip>(), plan(days).tips)
        val screens = days.map { it.copy(sleepFactors = it.sleepFactors + SleepFactor.Screens) }
        assertEquals(listOf(SleepCoachTip.Screens), plan(screens).tips)
    }

    @Test fun `nights shorter than the goal suggest keeping the wake time and going to bed sleepy`() {
        val days = (0 until 7).map { night(it.toLong(), hours = 6f) }
        assertTrue(SleepCoachTip.ShortNights in plan(days).tips)
    }

    @Test fun `nights at the goal give no short-nights tip`() {
        val days = (0 until 7).map { night(it.toLong()) }
        assertTrue(SleepCoachTip.ShortNights !in plan(days).tips)
    }

    @Test fun `at most three tips are shown`() {
        val days = (0 until 9).map {
            night(
                it.toLong(),
                hours = 5f,
                factors = setOf(SleepFactor.LateCaffeine, SleepFactor.Screens, SleepFactor.Stress),
            ).copy(
                bedtime = if (it % 2 == 0) LocalTime.of(20, 30) else goal.bedtime,
                wakeTime = LocalTime.of(9, 0),
            )
        }
        assertEquals(3, plan(days).tips.size)
    }

    @Test fun `a codec round-trips the preferences, and older or broken text gives none`() {
        val preferences = goal.copy(
            bedtime = LocalTime.of(23, 5),
            wakeTime = LocalTime.of(5, 45),
            windDown = setOf(WindDownStep.WorryNote, WindDownStep.ScreensOff),
        )
        assertEquals(preferences, SleepCoachPreferencesCodec.decode(SleepCoachPreferencesCodec.encode(preferences)))
        assertNull(SleepCoachPreferencesCodec.decode(null))
        assertNull(SleepCoachPreferencesCodec.decode(""))
        assertNull(SleepCoachPreferencesCodec.decode("s0;1;2;3"))
        assertNull(SleepCoachPreferencesCodec.decode("s1;13:50;390;ScreensOff"))
        assertNull(SleepCoachPreferencesCodec.decode("s1;9999;390;ScreensOff"))
        // No steps is a real choice: the coach runs on the goals alone until the user picks some.
        assertEquals(goal, SleepCoachPreferencesCodec.decode("s1;1350;390;"))
    }

    @Test fun `unknown wind-down steps from a later version are skipped`() {
        assertEquals(
            goal.copy(windDown = setOf(WindDownStep.Read)),
            SleepCoachPreferencesCodec.decode("s1;1350;390;Read,Meditate"),
        )
    }
}
