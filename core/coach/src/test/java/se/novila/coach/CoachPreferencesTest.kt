package se.novila.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import se.novila.anatomy.Equipment
import java.time.DayOfWeek

class CoachPreferencesTest {
    @Test fun `preferences survive encoding`() {
        val preferences = CoachPreferences(
            goal = CoachGoal.Endurance,
            daysPerWeek = 5,
            sessionMinutes = 45,
            focus = setOf(BodyArea.Legs, BodyArea.Core),
            considerations = setOf(Consideration.Quiet),
            variation = 3,
        )
        assertEquals(preferences, CoachPreferencesCodec.decode(CoachPreferencesCodec.encode(preferences)))
        assertEquals(CoachPreferences(), CoachPreferencesCodec.decode(CoachPreferencesCodec.encode(CoachPreferences())))
    }

    @Test fun `rehab answers survive encoding, and answers from before rehab are still read`() {
        val rehab = CoachPreferences(goal = CoachGoal.Rehab, rehabArea = RehabArea.LowerBack, rehabPhase = 2)
        assertEquals(rehab, CoachPreferencesCodec.decode(CoachPreferencesCodec.encode(rehab)))
        val shoulder = CoachPreferences(goal = CoachGoal.Rehab, rehabArea = RehabArea.Shoulder, rehabPhase = 1)
        assertEquals(shoulder, CoachPreferencesCodec.decode(CoachPreferencesCodec.encode(shoulder)))
        RehabArea.entries.forEach { area ->
            val preferences = CoachPreferences(goal = CoachGoal.Rehab, rehabArea = area, rehabPhase = 3)
            assertEquals(preferences, CoachPreferencesCodec.decode(CoachPreferencesCodec.encode(preferences)))
        }
        assertEquals(
            CoachPreferences(goal = CoachGoal.Strength, daysPerWeek = 5, focus = setOf(BodyArea.Core)),
            CoachPreferencesCodec.decode("c1;Strength;5;30;Core;;0"),
        )
        assertEquals(
            CoachPreferences.MAX_REHAB_PHASE,
            CoachPreferencesCodec.decode("c2;Rehab;4;30;;;0;Knee;9")?.rehabPhase,
        )
    }

    @Test fun `equipment survives encoding, and answers from before equipment are still read`() {
        val equipped = CoachPreferences(equipment = setOf(Equipment.Band, Equipment.Dumbbells))
        assertEquals(equipped, CoachPreferencesCodec.decode(CoachPreferencesCodec.encode(equipped)))
        assertEquals(
            CoachPreferences(goal = CoachGoal.Rehab, rehabArea = RehabArea.Hip, rehabPhase = 2),
            CoachPreferencesCodec.decode("c2;Rehab;4;30;;;0;Hip;2"),
        )
        assertEquals(
            setOf(Equipment.Barbell),
            CoachPreferencesCodec.decode("c3;Health;4;30;;;0;Knee;1;Barbell,Kettlebell")?.equipment,
        )
        assertEquals(setOf(Equipment.Band), CoachPreferences().toggle(Equipment.Band).equipment)
        assertEquals(emptySet<Equipment>(), CoachPreferences().toggle(Equipment.Band).toggle(Equipment.Band).equipment)
    }

    @Test fun `nothing stored or unreadable means no coach`() {
        assertNull(CoachPreferencesCodec.decode(null))
        assertNull(CoachPreferencesCodec.decode(""))
        assertNull(CoachPreferencesCodec.decode("c2;Health;4;30;;;0"))
        assertNull(CoachPreferencesCodec.decode("c3;Health;4;30;;;0;Knee;1"))
        assertNull(CoachPreferencesCodec.decode("c1;Yoga;4;30;;;0"))
        assertNull(CoachPreferencesCodec.decode("c1;Health;four;30;;;0"))
    }

    @Test fun `unknown names are skipped and numbers kept in range`() {
        val decoded = CoachPreferencesCodec.decode("c1;Strength;9;7;Legs,Arms;Back,Wrists;-2")
        assertEquals(
            CoachPreferences(
                goal = CoachGoal.Strength,
                daysPerWeek = CoachPreferences.MAX_DAYS,
                sessionMinutes = CoachPreferences.MIN_MINUTES,
                focus = setOf(BodyArea.Legs),
                considerations = setOf(Consideration.Back),
                variation = 0,
            ),
            decoded,
        )
    }

    @Test fun `own days survive encoding, and answers from before own days are still read`() {
        val own = CoachPreferences().toggle(DayOfWeek.SUNDAY)
        assertEquals(own, CoachPreferencesCodec.decode(CoachPreferencesCodec.encode(own)))
        assertEquals(
            CoachPreferences(daysPerWeek = 2),
            CoachPreferencesCodec.decode("c3;Health;2;30;;;0;Knee;1;"),
        )
    }

    @Test fun `own days start from the spread days, and the count follows them`() {
        val spread = CoachPreferences(daysPerWeek = 3)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), spread.trainingDays)
        val own = spread.toggle(DayOfWeek.WEDNESDAY).toggle(DayOfWeek.SATURDAY)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY), own.trainingDays)
        assertEquals(3, own.daysPerWeek)
        assertEquals(2, own.toggle(DayOfWeek.MONDAY).daysPerWeek)
        // The last day stays, and a seventh day is one too many.
        val one = CoachPreferences(daysPerWeek = 1)
        assertEquals(one, one.toggle(one.trainingDays.single()))
        val six = CoachPreferences(daysPerWeek = 6)
        assertEquals(six, six.toggle(DayOfWeek.SUNDAY))
        // A new number of days is spread out again.
        assertEquals(emptySet<DayOfWeek>(), own.withDays(4).days)
        assertEquals(4, own.withDays(4).trainingDays.size)
        assertEquals(spread.trainingDays, own.withSpreadDays().withDays(3).trainingDays)
    }

    @Test fun `answers change one at a time`() {
        val preferences = CoachPreferences()
        assertEquals(1, preferences.withDays(1).daysPerWeek)
        assertEquals(CoachPreferences.MIN_DAYS, preferences.withDays(0).daysPerWeek)
        assertEquals(35, preferences.withMinutes(37).sessionMinutes)
        assertEquals(CoachPreferences.MAX_MINUTES, preferences.withMinutes(90).sessionMinutes)
        assertEquals(setOf(BodyArea.Legs), preferences.toggle(BodyArea.Legs).focus)
        assertEquals(emptySet<BodyArea>(), preferences.toggle(BodyArea.Legs).toggle(BodyArea.Legs).focus)
        assertEquals(setOf(Consideration.Knees), preferences.toggle(Consideration.Knees).considerations)
        assertEquals(1, preferences.nextVariation().variation)
    }
}
