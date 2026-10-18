package se.novila.feature.today

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.MAX_DIARY_LENGTH

@RunWith(RobolectricTestRunner::class)
class MoodCardTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(
        mood: Int = 0,
        energy: Int = 0,
        stress: Int = 0,
        diary: String = "",
        onMood: (Int) -> Unit = {},
        onEnergy: (Int) -> Unit = {},
        onStress: (Int) -> Unit = {},
        onDiary: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            NovilaTheme {
                MoodCard(mood, energy, stress, diary, onMood, onEnergy, onStress, onDiary)
            }
        }
    }

    @Test fun `the three scales report the tapped level`() {
        val moods = mutableListOf<Int>()
        val energies = mutableListOf<Int>()
        val stresses = mutableListOf<Int>()
        show(onMood = { moods += it }, onEnergy = { energies += it }, onStress = { stresses += it })
        composeRule.onAllNodesWithContentDescription("Great")[0].performClick()
        composeRule.onAllNodesWithContentDescription("Full of energy")[0].performClick()
        composeRule.onAllNodesWithContentDescription("Very stressed")[0].performClick()
        assertEquals(listOf(5), moods)
        assertEquals(listOf(5), energies)
        assertEquals(listOf(5), stresses)
    }

    @Test fun `the scales show the logged levels`() {
        show(mood = 4, energy = 5, stress = 1)
        composeRule.onAllNodesWithContentDescription("Good")[0].assertIsSelected()
        composeRule.onNodeWithContentDescription("Full of energy").assertIsSelected()
        composeRule.onNodeWithContentDescription("Calm").assertIsSelected()
    }

    @Test fun `the diary line shows its hint and reports what is written`() {
        var written = ""
        show(onDiary = { written = it })
        composeRule.onNodeWithText("One line about your day").assertExists()
        composeRule.onNode(hasSetTextAction()).performTextInput("Long walk")
        assertEquals("Long walk", written)
    }

    @Test fun `the diary line never grows past the length the model keeps`() {
        var written = ""
        show(onDiary = { written = it })
        composeRule.onNode(hasSetTextAction()).performTextInput("x".repeat(MAX_DIARY_LENGTH + 40))
        composeRule.runOnIdle { assertEquals(MAX_DIARY_LENGTH, written.length) }
    }
}
