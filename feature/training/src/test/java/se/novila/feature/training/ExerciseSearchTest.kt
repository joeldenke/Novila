package se.novila.feature.training

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import se.novila.anatomy.ExerciseLibrary
import se.novila.coach.BodyArea
import java.util.Locale

class ExerciseSearchTest {
    private val sv = Locale.forLanguageTag("sv-SE")
    private val squat = listOf("Knäböj", "lår", "säte")

    @Test fun `every word must be found, in any of the texts and any case`() {
        assertTrue(matchesSearch("", squat, sv))
        assertTrue(matchesSearch("  KNÄ ", squat, sv))
        assertTrue(matchesSearch("böj säte", squat, sv))
        assertFalse(matchesSearch("knäböj bröst", squat, sv))
    }

    @Test fun `body areas filter on the main muscles`() {
        val all = ExerciseLibrary.all
        assertEquals(all, all.filter { trains(it, null) })
        val legs = all.filter { trains(it, BodyArea.Legs) }.map { it.id }
        assertTrue("squat" in legs)
        assertFalse("push_up" in legs)
        assertTrue("push_up" in all.filter { trains(it, BodyArea.UpperBody) }.map { it.id })
    }
}
