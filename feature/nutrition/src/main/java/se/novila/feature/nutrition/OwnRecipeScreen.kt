package se.novila.feature.nutrition

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaActionChip
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaConfirmDialog
import se.novila.designsystem.component.NovilaIconButton
import se.novila.designsystem.component.NovilaListRow
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaStepper
import se.novila.designsystem.component.NovilaSwitchRow
import se.novila.designsystem.component.NovilaTextButton
import se.novila.designsystem.component.NovilaTextField
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.FoodGroup
import se.novila.recipes.Allergen
import se.novila.recipes.MealSlot
import se.novila.recipes.OwnRecipe
import se.novila.recipes.OwnRecipeDraft
import se.novila.recipes.completeIngredient
import se.novila.recipes.completedIngredient
import se.novila.ui.common.ScreenList
import se.novila.ui.common.allergenName

/** What the recipe editor can do: change the draft, save it and delete the saved recipe. */
data class OwnRecipeActions(
    val onChange: ((OwnRecipeDraft) -> OwnRecipeDraft) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

/**
 * Writes a recipe of the user's own: name, meal and time, then the ingredients one at a time with foods from the
 * dictionary offered as the user types, and the steps. What a serving gives is counted from the ingredients as they
 * are added, and the allergens found in them are chosen up front; the user checks them before the recipe can be
 * saved. [new] is false when an existing recipe is changed, which can then also be deleted.
 */
@Composable
fun OwnRecipeScreen(
    draft: OwnRecipeDraft,
    new: Boolean,
    actions: OwnRecipeActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    initialIngredient: String = "",
) {
    var deleting by rememberSaveable { mutableStateOf(false) }
    val onChange = actions.onChange
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "about") { AboutCard(draft.recipe, onChange) }
        item(key = "ingredients") { IngredientsCard(draft.recipe, onChange, initialIngredient) }
        item(key = "steps") { StepsCard(draft.recipe, onChange) }
        item(key = "allergens") { AllergensCard(draft, onChange) }
        item(key = "save") {
            NovilaAccentButton(
                text = stringResource(R.string.own_recipe_save),
                onClick = actions.onSave,
                enabled = draft.canSave,
            )
        }
        if (!new) {
            item(key = "delete") {
                NovilaTextButton(
                    text = stringResource(R.string.own_recipe_delete),
                    onClick = { deleting = true },
                    role = NovilaTheme.pillars.nutrition,
                )
            }
        }
    }
    if (deleting) {
        NovilaConfirmDialog(
            title = stringResource(R.string.own_recipe_delete_title),
            text = stringResource(R.string.own_recipe_delete_text, draft.recipe.name),
            confirmLabel = stringResource(R.string.own_recipe_delete_confirm),
            dismissLabel = stringResource(R.string.own_recipe_delete_cancel),
            onConfirm = {
                deleting = false
                actions.onDelete()
            },
            onDismiss = { deleting = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutCard(recipe: OwnRecipe, onChange: ((OwnRecipeDraft) -> OwnRecipeDraft) -> Unit) {
    val nutrition = NovilaTheme.pillars.nutrition
    NovilaCard(title = stringResource(R.string.own_recipe_about), role = nutrition) {
        NovilaTextField(
            value = recipe.name,
            onValueChange = { name ->
                onChange { it.edited { recipe -> recipe.copy(name = name.take(OwnRecipe.MAX_NAME_LENGTH)) } }
            },
            label = stringResource(R.string.own_recipe_name),
            role = nutrition,
        )
        Heading(stringResource(R.string.own_recipe_when))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        ) {
            MealSlot.entries.forEach { slot ->
                NovilaChoiceChip(
                    label = mealSlotName(slot),
                    selected = recipe.slot == slot,
                    onClick = { onChange { it.edited { r -> r.copy(slot = slot) } } },
                    role = nutrition,
                )
            }
        }
        NovilaStepper(
            title = stringResource(R.string.own_recipe_minutes),
            value = stringResource(R.string.recipe_minutes, recipe.minutes),
            onDecrement = { onChange { it.edited { r -> r.copy(minutes = r.minutes - MINUTES_STEP) } } },
            onIncrement = { onChange { it.edited { r -> r.copy(minutes = r.minutes + MINUTES_STEP) } } },
            decrementDescription = stringResource(R.string.own_recipe_minutes_less),
            incrementDescription = stringResource(R.string.own_recipe_minutes_more),
            role = nutrition,
            decrementEnabled = recipe.minutes > MINUTES_STEP,
            incrementEnabled = recipe.minutes < OwnRecipe.MAX_MINUTES,
        )
    }
}

/** The time changes five minutes at a time, from five minutes up to [OwnRecipe.MAX_MINUTES]. */
private const val MINUTES_STEP = 5

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientsCard(
    recipe: OwnRecipe,
    onChange: ((OwnRecipeDraft) -> OwnRecipeDraft) -> Unit,
    initialIngredient: String,
) {
    val nutrition = NovilaTheme.pillars.nutrition
    var input by rememberSaveable { mutableStateOf(initialIngredient) }
    val completions = remember(input) { completeIngredient(input) }
    NovilaCard(title = stringResource(R.string.recipe_ingredients), role = nutrition) {
        Hint(stringResource(R.string.own_recipe_ingredients_hint))
        recipe.ingredients.forEachIndexed { index, line ->
            RemovableRow(line, onRemove = { onChange { it.withoutIngredient(index) } })
        }
        if (recipe.ingredients.size < OwnRecipe.MAX_LINES) {
            NovilaTextField(
                value = input,
                onValueChange = { input = it.take(OwnRecipe.MAX_LINE_LENGTH) },
                label = stringResource(R.string.own_recipe_add_ingredient),
                role = nutrition,
            )
            if (completions.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
                    verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
                ) {
                    completions.forEach { word ->
                        NovilaActionChip(
                            label = word,
                            onClick = { input = completedIngredient(input, word) },
                            role = nutrition,
                        )
                    }
                }
            }
            NovilaButton(
                text = stringResource(R.string.own_recipe_add),
                onClick = {
                    onChange { it.withIngredient(input) }
                    input = ""
                },
                enabled = input.isNotBlank(),
                role = nutrition,
            )
        }
        Hint(givesText(recipe))
        val unknown = recipe.contents.unknown.joinToString()
        if (unknown.isNotEmpty()) Hint(stringResource(R.string.meal_log_unknown, unknown))
    }
}

/** "Gives: Vegetables & fruit 2, Meals 1", counted from the ingredients, or that nothing is filled yet. */
@Composable
private fun givesText(recipe: OwnRecipe): String {
    val portions = FoodGroup.entries
        .filter { it != FoodGroup.Meals && recipe.portions(it) > 0 }
        .map { stringResource(R.string.recipe_portion, foodName(it), recipe.portions(it)) }
    val treats = recipe.contents.treats.takeIf { it > 0 }
        ?.let { stringResource(R.string.recipe_portion, stringResource(R.string.food_treats), it) }
    val redMeat = recipe.contents.redMeat.takeIf { it > 0 }
        ?.let { stringResource(R.string.recipe_portion, stringResource(R.string.food_red_meat), it) }
    val gives = portions + listOfNotNull(redMeat, treats)
    return if (gives.isEmpty()) {
        stringResource(R.string.own_recipe_gives_nothing)
    } else {
        stringResource(R.string.own_recipe_gives, gives.joinToString(", "))
    }
}

@Composable
private fun StepsCard(recipe: OwnRecipe, onChange: ((OwnRecipeDraft) -> OwnRecipeDraft) -> Unit) {
    val nutrition = NovilaTheme.pillars.nutrition
    var input by rememberSaveable { mutableStateOf("") }
    NovilaCard(title = stringResource(R.string.recipe_steps), role = nutrition) {
        recipe.steps.forEachIndexed { index, step ->
            RemovableRow(
                stringResource(R.string.recipe_step, index + 1, step),
                onRemove = { onChange { it.withoutStep(index) } },
            )
        }
        if (recipe.steps.size < OwnRecipe.MAX_LINES) {
            NovilaTextField(
                value = input,
                onValueChange = { input = it.take(OwnRecipe.MAX_STEP_LENGTH) },
                label = stringResource(R.string.own_recipe_add_step),
                role = nutrition,
            )
            NovilaButton(
                text = stringResource(R.string.own_recipe_add),
                onClick = {
                    onChange { it.withStep(input) }
                    input = ""
                },
                enabled = input.isNotBlank(),
                role = nutrition,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllergensCard(draft: OwnRecipeDraft, onChange: ((OwnRecipeDraft) -> OwnRecipeDraft) -> Unit) {
    val nutrition = NovilaTheme.pillars.nutrition
    NovilaCard(title = stringResource(R.string.own_recipe_allergens), role = nutrition) {
        val found = draft.found
        Hint(
            if (found.isEmpty()) {
                stringResource(R.string.own_recipe_allergens_none_found)
            } else {
                stringResource(
                    R.string.own_recipe_allergens_found,
                    found.sorted().map { allergenName(it) }.joinToString(", "),
                )
            },
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        ) {
            Allergen.entries.forEach { allergen ->
                NovilaChoiceChip(
                    label = allergenName(allergen),
                    selected = allergen in draft.allergens,
                    onClick = { onChange { it.toggled(allergen) } },
                    role = nutrition,
                )
            }
        }
        NovilaSwitchRow(
            title = stringResource(R.string.own_recipe_allergens_checked),
            summary = allergensText(draft.allergens),
            checked = draft.checked,
            onCheckedChange = { checked -> onChange { it.copy(checked = checked) } },
            role = nutrition,
        )
    }
}

/** A line of the recipe with a cross that takes it away. */
@Composable
private fun RemovableRow(text: String, onRemove: () -> Unit) {
    NovilaListRow(
        title = text,
        trailing = {
            NovilaIconButton(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.own_recipe_remove, text),
                onClick = onRemove,
            )
        },
    )
}

/** What a meal slot is called: breakfast, lunch or dinner, or a snack. */
@Composable
internal fun mealSlotName(slot: MealSlot): String = stringResource(
    when (slot) {
        MealSlot.Breakfast -> R.string.meal_slot_breakfast
        MealSlot.Main -> R.string.meal_slot_main
        MealSlot.Snack -> R.string.meal_slot_snack
    },
)

@Composable
private fun OwnRecipeContent(draft: OwnRecipeDraft, new: Boolean, initialIngredient: String = "") {
    NovilaTheme {
        NovilaScaffold { padding ->
            OwnRecipeScreen(
                draft = draft,
                new = new,
                actions = OwnRecipeActions(),
                contentPadding = padding,
                initialIngredient = initialIngredient,
            )
        }
    }
}

/** A new recipe, half written: the last ingredient is being typed and foods are offered for it. */
@Preview(heightDp = 1800)
@Composable
private fun OwnRecipeNewPreview() = OwnRecipeContent(
    OwnRecipeDraft()
        .edited { it.copy(name = "Pannkakor", slot = MealSlot.Breakfast, minutes = 30) }
        .withIngredient("3 dl vetemjöl")
        .withIngredient("6 dl mjölk")
        .withIngredient("3 ägg")
        .withStep("Vispa ihop mjöl och hälften av mjölken till en slät smet."),
    new = true,
    initialIngredient = "2 dl blå",
)

@Preview(heightDp = 1800, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun OwnRecipeEditPreview() = OwnRecipeContent(
    OwnRecipeDraft.of(
        OwnRecipe(
            name = "Mormors linssoppa",
            minutes = 30,
            ingredients = listOf("2 dl röda linser", "1 morot", "1 burk krossade tomater", "1 dl grädde"),
            steps = listOf("Koka linser och morot i 20 minuter.", "Rör ner tomater och grädde."),
            allergens = setOf(Allergen.Milk, Allergen.Celery),
            id = 1,
        ),
    ),
    new = false,
)
