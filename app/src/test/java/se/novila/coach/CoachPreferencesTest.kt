package se.novila.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
        assertEquals(
            CoachPreferences(goal = CoachGoal.Strength, daysPerWeek = 5, focus = setOf(BodyArea.Core)),
            CoachPreferencesCodec.decode("c1;Strength;5;30;Core;;0"),
        )
        assertEquals(
            CoachPreferences.MAX_REHAB_PHASE,
            CoachPreferencesCodec.decode("c2;Rehab;4;30;;;0;Knee;9")?.rehabPhase,
        )
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

    @Test fun `answers change one at a time`() {
        val preferences = CoachPreferences()
        assertEquals(CoachPreferences.MIN_DAYS, preferences.withDays(1).daysPerWeek)
        assertEquals(35, preferences.withMinutes(37).sessionMinutes)
        assertEquals(CoachPreferences.MAX_MINUTES, preferences.withMinutes(90).sessionMinutes)
        assertEquals(setOf(BodyArea.Legs), preferences.toggle(BodyArea.Legs).focus)
        assertEquals(emptySet<BodyArea>(), preferences.toggle(BodyArea.Legs).toggle(BodyArea.Legs).focus)
        assertEquals(setOf(Consideration.Knees), preferences.toggle(Consideration.Knees).considerations)
        assertEquals(1, preferences.nextVariation().variation)
    }
}
