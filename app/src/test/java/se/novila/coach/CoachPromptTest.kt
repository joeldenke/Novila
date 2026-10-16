package se.novila.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class CoachPromptTest {
    private val today = LocalDate.of(2026, 10, 15)
    private val names = mapOf("squat" to "Knäböj", "push_up" to "Armhävning", "glute_bridge" to "Höftlyft")

    private fun prompt(preferences: CoachPreferences, locale: Locale = Locale.forLanguageTag("sv-SE")) =
        CoachPrompt.build(
            CoachPlanner.plan(preferences, emptyList(), today),
            preferences,
            CoachPromptInput(locale, names, listOf("Minst en vilodag mellan styrkedagarna.")),
        )

    @Test fun `the prompt holds the plan, the reasons and the user's language`() {
        val sv = prompt(CoachPreferences())
        assertTrue(sv, "Monday" in sv && "Knäböj" in sv && "Armhävning" in sv)
        assertTrue(sv, "Minst en vilodag" in sv)
        assertTrue(sv, "in Swedish" in sv)
        assertTrue(sv, "do not add or change anything" in sv)
        assertFalse("rehab" in sv)
        assertTrue("in English" in prompt(CoachPreferences(), Locale.UK))
    }

    @Test fun `a rehab prompt names the area and the phase and asks for caution`() {
        val rehab = prompt(CoachPreferences(goal = CoachGoal.Rehab, rehabArea = RehabArea.LowerBack, rehabPhase = 2))
        assertTrue(rehab, "rehab for the lower back, phase 2 of 3" in rehab)
        assertTrue(rehab, "physiotherapist" in rehab && "Never diagnose" in rehab)
    }

    @Test fun `answers lose emojis and markdown and stay short`() {
        assertEquals(
            "Bra vecka! Börja lugnt.",
            CoachPrompt.clean("**Bra vecka!** 💪\n\nBörja  lugnt. ☀️"),
        )
        val long = "En mening som är lagom lång. ".repeat(60)
        val cleaned = CoachPrompt.clean(long)
        assertTrue(cleaned.length <= CoachPrompt.MAX_LENGTH)
        assertTrue(cleaned.endsWith("."))
    }
}
