package se.novila.feature.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.settings.R
import se.novila.recipes.Allergen
import se.novila.recipes.Diet
import se.novila.recipes.FoodPreferences
import se.novila.ui.common.ScreenList
import se.novila.ui.common.allergenName
import se.novila.ui.common.dietName
import se.novila.ui.common.maxMinutesText
import se.novila.ui.R as UiR

/**
 * What the Nutrition tab's recipe suggestions take into account: the user's diet, the allergens they avoid and how
 * long a recipe may take. [onChange] gets how to change the preferences, and each change is saved at once.
 */
@Composable
fun FoodPreferencesScreen(
    preferences: FoodPreferences,
    onChange: ((FoodPreferences) -> FoodPreferences) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "diet") {
            ChoiceCard(
                stringResource(R.string.food_preferences_diet),
                stringResource(R.string.food_preferences_diet_hint),
            ) {
                Diet.entries.forEach { diet ->
                    Chip(dietName(diet), preferences.diet == diet) { onChange { it.copy(diet = diet) } }
                }
            }
        }
        item(key = "allergens") {
            ChoiceCard(
                stringResource(R.string.food_preferences_allergens),
                stringResource(R.string.food_preferences_allergens_hint),
            ) {
                Allergen.entries.forEach { allergen ->
                    Chip(allergenName(allergen), allergen in preferences.allergens) { onChange { it.toggle(allergen) } }
                }
            }
        }
        item(key = "time") {
            ChoiceCard(
                stringResource(R.string.food_preferences_time),
                stringResource(R.string.food_preferences_time_hint),
            ) {
                (listOf(null) + FoodPreferences.MINUTES).forEach { minutes ->
                    Chip(maxMinutesText(minutes), preferences.maxMinutes == minutes) {
                        onChange { it.withMaxMinutes(minutes) }
                    }
                }
            }
        }
        item(key = "about") {
            NovilaText(
                stringResource(UiR.string.food_check_ingredients),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceCard(title: String, hint: String, chips: @Composable () -> Unit) {
    NovilaCard(title = title) {
        NovilaText(hint, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        ) { chips() }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    NovilaChoiceChip(label = label, selected = selected, onClick = onClick, role = NovilaTheme.pillars.nutrition)
}

@Composable
private fun FoodPreferencesContent(preferences: FoodPreferences) {
    NovilaTheme {
        NovilaScaffold { padding ->
            FoodPreferencesScreen(preferences = preferences, onChange = {}, contentPadding = padding)
        }
    }
}

@Preview(heightDp = 1000)
@Composable
private fun FoodPreferencesPreview() = FoodPreferencesContent(FoodPreferences())

@Preview(heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun FoodPreferencesDarkPreview() = FoodPreferencesContent(
    FoodPreferences(Diet.Vegetarian, setOf(Allergen.Peanuts, Allergen.TreeNuts), maxMinutes = 30),
)
