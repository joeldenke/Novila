package se.novila.feature.nutrition.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import se.novila.feature.nutrition.NutritionScreen
import se.novila.model.DayEntry
import se.novila.model.FoodGroup

/** The Nutrition tab. */
@Serializable data object NutritionRoute : NavKey

fun EntryProviderScope<NavKey>.nutritionEntry(
    today: DayEntry,
    history: List<DayEntry>,
    onPortions: (FoodGroup, Int) -> Unit,
    onTreats: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    entry<NutritionRoute> { NutritionScreen(today, history, onPortions, onTreats, contentPadding) }
}
