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
import se.novila.recipes.Allergen
import se.novila.recipes.FoodPreferences
import se.novila.recipes.MealSlot
import se.novila.recipes.OwnRecipe
import se.novila.recipes.RecipeSuggestions
import se.novila.recipes.key
import se.novila.ui.common.allergenName
import se.novila.ui.common.foodPreferencesSummary
import se.novila.ui.R as UiR

/**
 * Recipes for what today is still missing, chosen for the user's [preferences]: their own first, then Novila's.
 * Tapping a recipe shows its ingredients and steps ([expanded] is the open recipe's key, [onExpand]), and [actions]
 * log one serving of it.
 */
@Composable
fun RecipeCard(
    suggestions: RecipeSuggestions,
    preferences: FoodPreferences,
    expanded: String?,
    onExpand: (String?) -> Unit,
    actions: RecipeActions,
) {
    val nutrition = NovilaTheme.pillars.nutrition
    NovilaCard(
        title = stringResource(R.string.recipes_title),
        icon = painterResource(UiR.drawable.ic_nutrition),
        role = nutrition,
    ) {
        Hint(
            stringResource(
                if (suggestions.recipes.isEmpty() && suggestions.own.isEmpty()) {
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
        suggestions.own.forEach { (recipe, points) ->
            ExpandableRecipe(
                title = recipe.name,
                summary = stringResource(R.string.own_recipe_summary, summary(recipe.minutes, points)),
                expanded = expanded == recipe.key,
                onExpand = { onExpand(if (it) recipe.key else null) },
            ) {
                OwnRecipeDetails(recipe, onLog = { actions.onLogOwn(recipe) })
            }
        }
        suggestions.recipes.forEach { (recipe, points) ->
            ExpandableRecipe(
                title = stringResource(recipe.text.name),
                summary = summary(recipe.minutes, points),
                expanded = expanded == recipe.key,
                onExpand = { onExpand(if (it) recipe.key else null) },
            ) {
                val text = recipe.text
                RecipeDetails(
                    portions = FoodGroup.entries.associateWith(recipe::portions),
                    allergens = recipe.allergens,
                    ingredients = stringArrayResource(text.ingredients).toList(),
                    steps = stringArrayResource(text.steps).toList(),
                    onLog = { actions.onLog(recipe) },
                    redMeat = recipe.redMeat,
                    fish = recipe.fish,
                )
            }
        }
        Hint(foodPreferencesSummary(preferences))
        NovilaTextButton(
            text = stringResource(R.string.recipes_preferences),
            onClick = actions.onOpenPreferences,
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
private fun summary(minutes: Int, points: Int): String = if (points > 0) {
    stringResource(R.string.recipe_summary, minutes, points)
} else {
    stringResource(R.string.recipe_minutes, minutes)
}

/** A recipe as a row that opens to show its [details] below it. */
@Composable
private fun ExpandableRecipe(
    title: String,
    summary: String,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    details: @Composable () -> Unit,
) {
    NovilaListRow(
        title = title,
        summary = summary,
        onClick = { onExpand(!expanded) },
        onClickLabel = stringResource(if (expanded) R.string.recipe_collapse else R.string.recipe_expand),
        stateDescription = stringResource(if (expanded) R.string.recipe_expanded else R.string.recipe_collapsed),
    )
    if (expanded) details()
}

/** One of the user's own recipes, opened: what its ingredients give, its confirmed allergens and the text. */
@Composable
private fun OwnRecipeDetails(recipe: OwnRecipe, onLog: () -> Unit) {
    RecipeDetails(
        portions = FoodGroup.entries.associateWith(recipe::portions),
        allergens = recipe.allergens,
        ingredients = recipe.ingredients,
        steps = recipe.steps,
        onLog = onLog,
        treats = recipe.contents.treats,
        redMeat = recipe.contents.redMeat,
        fish = recipe.contents.fish,
    )
}

/** What one serving gives, its allergens, ingredients and steps, and a button to log it. */
@Composable
private fun RecipeDetails(
    portions: Map<FoodGroup, Int>,
    allergens: Set<Allergen>,
    ingredients: List<String>,
    steps: List<String>,
    onLog: () -> Unit,
    treats: Int = 0,
    redMeat: Int = 0,
    fish: Int = 0,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
        val gives = portions.filterValues { it > 0 }
            .map { (group, count) -> stringResource(R.string.recipe_portion, foodName(group), count) } +
            listOfNotNull(
                fish.takeIf { it > 0 }?.let {
                    stringResource(R.string.recipe_portion, stringResource(R.string.food_fish), it)
                },
                redMeat.takeIf { it > 0 }?.let {
                    stringResource(R.string.recipe_portion, stringResource(R.string.food_red_meat), it)
                },
                treats.takeIf { it > 0 }?.let {
                    stringResource(R.string.recipe_portion, stringResource(R.string.food_treats), it)
                },
            )
        if (gives.isNotEmpty()) Hint(stringResource(R.string.recipe_gives, gives.joinToString(", ")))
        Hint(allergensText(allergens))
        Heading(stringResource(R.string.recipe_ingredients))
        ingredients.forEach { Body(stringResource(R.string.recipe_ingredient, it)) }
        if (steps.isNotEmpty()) {
            Heading(stringResource(R.string.recipe_steps))
            steps.forEachIndexed { index, step -> Body(stringResource(R.string.recipe_step, index + 1, step)) }
        }
        NovilaButton(text = stringResource(R.string.recipe_log), onClick = onLog, role = NovilaTheme.pillars.nutrition)
    }
}

/** "Contains: gluten, milk", or that it has none of the labelled allergens. */
@Composable
internal fun allergensText(allergens: Set<Allergen>): String = if (allergens.isEmpty()) {
    stringResource(R.string.recipe_contains_none)
} else {
    stringResource(R.string.recipe_contains, allergens.sorted().map { allergenName(it) }.joinToString(", "))
}

@Composable
internal fun Heading(text: String) {
    NovilaText(text, style = NovilaTheme.typography.labelLarge)
}

@Composable
internal fun Body(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium)
}

@Composable
internal fun Hint(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}
