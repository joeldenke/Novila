package se.novila.feature.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTextField
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.MealLog
import se.novila.model.parseMeal

/**
 * Meal logging in the user's own words: "Breakfast: porridge, berries" fills in the food groups it recognizes, one
 * portion per word. What the words translate into is shown before they are added, and words outside the dictionary
 * are listed so nothing is guessed at. The field starts with [initialText], which previews use to show a typed meal.
 */
@Composable
fun MealLogCard(
    onLogMeal: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialText: String = "",
) {
    val nutrition = NovilaTheme.pillars.nutrition
    var text by rememberSaveable { mutableStateOf(initialText) }
    val meal = remember(text) { parseMeal(text) }
    NovilaCard(modifier = modifier, title = stringResource(R.string.meal_log_title), role = nutrition) {
        Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m)) {
            NovilaTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.meal_log_label),
                supportingText = stringResource(R.string.meal_log_hint),
                last = true,
                role = nutrition,
            )
            if (!meal.isEmpty) {
                NovilaText(
                    stringResource(R.string.meal_log_preview, addsIn(meal)),
                    style = NovilaTheme.typography.bodyMedium,
                )
            }
            val unknown = meal.unknown.joinToString()
            if (unknown.isNotEmpty()) {
                NovilaText(
                    stringResource(R.string.meal_log_unknown, unknown),
                    style = NovilaTheme.typography.bodySmall,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )
            }
            NovilaButton(
                text = stringResource(R.string.meal_log_add),
                onClick = {
                    onLogMeal(text)
                    text = ""
                },
                enabled = !meal.isEmpty,
                role = nutrition,
            )
        }
    }
}

/** "Meals +1 · Whole grain +1 · Vegetables & fruit +1", in the order the groups are logged. */
@Composable
@ReadOnlyComposable
private fun addsIn(meal: MealLog): String {
    val portions = meal.portions.map { (group, count) -> stringResource(R.string.meal_portion, foodName(group), count) }
    val treats = if (meal.treats == 0) {
        emptyList()
    } else {
        listOf(stringResource(R.string.meal_portion, stringResource(R.string.food_treats), meal.treats))
    }
    return (portions + treats).joinToString(stringResource(R.string.meal_summary_separator))
}

@Preview(showBackground = true)
@Composable
private fun MealLogCardPreview() {
    NovilaTheme {
        MealLogCard(onLogMeal = {}, initialText = "Frukost: gröt, bär")
    }
}
