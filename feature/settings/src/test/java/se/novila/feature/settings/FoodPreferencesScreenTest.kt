package se.novila.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.recipes.Allergen
import se.novila.recipes.Diet
import se.novila.recipes.FoodPreferences

@RunWith(RobolectricTestRunner::class)
class FoodPreferencesScreenTest {
    @get:Rule val composeRule = createComposeRule()

    /** Shows the screen, holding the preferences as the ViewModel would, and returns them. */
    private fun show(): () -> FoodPreferences {
        var preferences by mutableStateOf(FoodPreferences())
        composeRule.setContent {
            NovilaTheme {
                FoodPreferencesScreen(
                    preferences = preferences,
                    onChange = { preferences = it(preferences) },
                    contentPadding = PaddingValues(),
                )
            }
        }
        return { preferences }
    }

    /** Scrolls the list to [text], so the lazy list composes it, and returns its node. */
    private fun scrollTo(text: String) = composeRule.run {
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        onNodeWithText(text)
    }

    @Test fun `a diet, allergens and a time limit can be chosen`() {
        val preferences = show()

        composeRule.onNodeWithText("Vegan").performClick()
        scrollTo("Sesame").performClick()
        scrollTo("Gluten").performClick()
        scrollTo("At most 15 min").performClick()

        assertEquals(FoodPreferences(Diet.Vegan, setOf(Allergen.Sesame, Allergen.Gluten), 15), preferences())
    }

    @Test fun `tapping an allergen again stops avoiding it`() {
        val preferences = show()

        scrollTo("Milk").performClick()
        scrollTo("Milk").performClick()

        assertEquals(emptySet<Allergen>(), preferences().allergens)
    }
}
