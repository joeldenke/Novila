package se.novila.feature.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaListRow
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTextButton
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.nutrition.R
import se.novila.model.FoodGroup
import se.novila.recipes.FoodPreferences
import se.novila.recipes.MealSlot
import se.novila.recipes.Recipe
import se.novila.recipes.RecipeSuggestion
import se.novila.recipes.RecipeSuggestions
import se.novila.ui.common.allergenName
import se.novila.ui.common.foodPreferencesSummary
import se.novila.ui.R as UiR

/**
 * Recipes for what today is still missing, chosen for the user's [preferences]. Tapping a recipe shows its
 * ingredients and steps ([expanded], [onExpand]), and [onLog] logs one serving of it.
 */
@Composable
fun RecipeCard(
    suggestions: RecipeSuggestions,
    preferences: FoodPreferences,
    expanded: Recipe?,
    onExpand: (Recipe?) -> Unit,
    onLog: (Recipe) -> Unit,
    onOpenPreferences: () -> Unit,
) {
    val nutrition = NovilaTheme.pillars.nutrition
    NovilaCard(
        title = stringResource(R.string.recipes_title),
        icon = painterResource(UiR.drawable.ic_nutrition),
        role = nutrition,
    ) {
        Hint(
            stringResource(
                if (suggestions.recipes.isEmpty()) {
                    R.string.recipes_none
                } else {
                    when (suggestions.slot) {
                        MealSlot.Breakfast -> R.string.recipes_breakfast
                        MealSlot.Main -> R.string.recipes_main
                        MealSlot.Snack -> R.string.recipes_snack
                    }
                },
            ),
        )
        suggestions.recipes.forEach { suggestion ->
            val recipe = suggestion.recipe
            val isExpanded = expanded == recipe
            NovilaListRow(
                title = stringResource(recipe.text.name),
                summary = summary(suggestion),
                onClick = { onExpand(if (isExpanded) null else recipe) },
                onClickLabel = stringResource(
                    if (isExpanded) R.string.recipe_collapse else R.string.recipe_expand,
                ),
                stateDescription = stringResource(
                    if (isExpanded) R.string.recipe_expanded else R.string.recipe_collapsed,
                ),
            )
            if (expanded == recipe) RecipeDetails(recipe, onLog = { onLog(recipe) })
        }
        Hint(foodPreferencesSummary(preferences))
        NovilaTextButton(
            text = stringResource(R.string.recipes_preferences),
            onClick = onOpenPreferences,
            role = nutrition,
        )
        NovilaText(
            stringResource(UiR.string.food_check_ingredients),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun summary(suggestion: RecipeSuggestion): String = if (suggestion.points > 0) {
    stringResource(R.string.recipe_summary, suggestion.recipe.minutes, suggestion.points)
} else {
    stringResource(R.string.recipe_minutes, suggestion.recipe.minutes)
}

/** What one serving gives, its allergens, ingredients and steps, and a button to log it. */
@Composable
private fun RecipeDetails(recipe: Recipe, onLog: () -> Unit) {
    val text = recipe.text
    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
        val portions = FoodGroup.entries
            .filter { recipe.portions(it) > 0 }
            .map { stringResource(R.string.recipe_portion, foodName(it), recipe.portions(it)) }
        Hint(stringResource(R.string.recipe_gives, portions.joinToString(", ")))
        Hint(
            if (recipe.allergens.isEmpty()) {
                stringResource(R.string.recipe_contains_none)
            } else {
                val names = recipe.allergens.sorted().map { allergenName(it) }
                stringResource(R.string.recipe_contains, names.joinToString(", "))
            },
        )
        Heading(stringResource(R.string.recipe_ingredients))
        stringArrayResource(text.ingredients).forEach { Body(stringResource(R.string.recipe_ingredient, it)) }
        Heading(stringResource(R.string.recipe_steps))
        stringArrayResource(text.steps).forEachIndexed { index, step ->
            Body(stringResource(R.string.recipe_step, index + 1, step))
        }
        NovilaButton(text = stringResource(R.string.recipe_log), onClick = onLog, role = NovilaTheme.pillars.nutrition)
    }
}

@Composable
private fun Heading(text: String) {
    NovilaText(text, style = NovilaTheme.typography.labelLarge)
}

@Composable
private fun Body(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium)
}

@Composable
private fun Hint(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}
