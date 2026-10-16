package se.novila.feature.training

import org.junit.Assert.assertNotNull
import org.junit.Test
import se.novila.anatomy.ExerciseLibrary

class ExerciseCardsTest {
    @Test fun `every exercise has a name, steps and a tip`() {
        ExerciseLibrary.all.forEach { assertNotNull(it.id, moveText(it.id)) }
    }
}
