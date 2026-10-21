package se.novila.feature.nutrition

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToKeyAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.recipes.Allergen
import se.novila.recipes.OwnRecipeDraft

@RunWith(RobolectricTestRunner::class)
class OwnRecipeScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private var draft by mutableStateOf(OwnRecipeDraft().edited { it.copy(name = "Pancakes") })
    private var saved = false

    private fun show() {
        composeRule.setContent {
            NovilaTheme {
                OwnRecipeScreen(
                    draft = draft,
                    new = true,
                    actions = OwnRecipeActions(onChange = { draft = it(draft) }, onSave = { saved = true }),
                    contentPadding = PaddingValues(),
                )
            }
        }
    }

    private fun scrollTo(key: String) = composeRule.onNode(hasScrollToKeyAction()).performScrollToKey(key)

    @Test fun `a food is completed as it is typed, and its allergen is chosen before the recipe is checked`() {
        show()
        scrollTo("ingredients")

        composeRule.onNode(hasSetTextAction() and hasText("Add an ingredient")).performTextInput("6 dl mil")
        composeRule.onNodeWithText("milk").performClick()
        composeRule.onAllNodesWithText("Add")[0].performClick()

        assertEquals(listOf("6 dl milk"), draft.recipe.ingredients)
        assertEquals(setOf(Allergen.Milk), draft.allergens)
        scrollTo("allergens")
        composeRule.onNodeWithText("Found in the ingredients: Milk", substring = true).assertExists()
        scrollTo("save")
        composeRule.onNodeWithText("Save recipe").assertIsNotEnabled()
    }

    @Test fun `the recipe is saved once the allergens are checked`() {
        draft = draft.withIngredient("2 dl lentils").withIngredient("1 carrot")
        show()
        scrollTo("ingredients")
        composeRule.onNodeWithText("One serving gives: Vegetables & fruit 1, Protein 1").assertExists()

        scrollTo("allergens")
        composeRule.onNodeWithText("I have checked the allergens").performClick()
        scrollTo("save")
        composeRule.onNodeWithText("Save recipe").assertIsEnabled().performClick()

        assertTrue(draft.checked)
        assertTrue(saved)
    }
}
