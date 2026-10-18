package se.novila.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import se.novila.recipes.Allergen
import se.novila.recipes.Diet
import se.novila.recipes.FoodPreferences
import se.novila.ui.R

@Composable
@ReadOnlyComposable
fun dietName(diet: Diet): String = stringResource(
    when (diet) {
        Diet.Everything -> R.string.diet_everything
        Diet.Pescetarian -> R.string.diet_pescetarian
        Diet.Vegetarian -> R.string.diet_vegetarian
        Diet.Vegan -> R.string.diet_vegan
    },
)

@Composable
@ReadOnlyComposable
fun allergenName(allergen: Allergen): String = stringResource(
    when (allergen) {
        Allergen.Gluten -> R.string.allergen_gluten
        Allergen.Crustaceans -> R.string.allergen_crustaceans
        Allergen.Eggs -> R.string.allergen_eggs
        Allergen.Fish -> R.string.allergen_fish
        Allergen.Peanuts -> R.string.allergen_peanuts
        Allergen.Soy -> R.string.allergen_soy
        Allergen.Milk -> R.string.allergen_milk
        Allergen.TreeNuts -> R.string.allergen_tree_nuts
        Allergen.Celery -> R.string.allergen_celery
        Allergen.Mustard -> R.string.allergen_mustard
        Allergen.Sesame -> R.string.allergen_sesame
        Allergen.Sulphites -> R.string.allergen_sulphites
        Allergen.Lupin -> R.string.allergen_lupin
        Allergen.Molluscs -> R.string.allergen_molluscs
    },
)

/** At most how long a recipe may take, or any time. */
@Composable
@ReadOnlyComposable
fun maxMinutesText(minutes: Int?): String =
    if (minutes == null) stringResource(R.string.food_time_any) else stringResource(R.string.food_time_max, minutes)

/** The preferences in one line, e.g. "Vegetarian · 2 allergens avoided · At most 30 min". */
@Composable
@ReadOnlyComposable
fun foodPreferencesSummary(preferences: FoodPreferences): String = stringResource(
    R.string.food_preferences_summary,
    dietName(preferences.diet),
    if (preferences.allergens.isEmpty()) {
        stringResource(R.string.food_allergens_none)
    } else {
        pluralStringResource(R.plurals.food_allergens_avoided, preferences.allergens.size, preferences.allergens.size)
    },
    maxMinutesText(preferences.maxMinutes),
)
