package se.novila.feature.nutrition.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.feature.nutrition.NutritionScreen
import se.novila.feature.nutrition.NutritionViewModel

/** The Nutrition tab. */
@Serializable data object NutritionRoute : NavKey

/** The Nutrition tab's entry, with its own [NutritionViewModel] scoped to the entry. */
fun EntryProviderScope<NavKey>.nutritionEntry(contentPadding: PaddingValues) {
    entry<NutritionRoute> {
        val viewModel = metroViewModel<NutritionViewModel>()
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        // Nothing to show until the saved days are read, a moment at app start.
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        NutritionScreen(
            today = state.week.today,
            history = state.week.history,
            goals = state.goals,
            onPortions = viewModel::changePortions,
            onTreats = viewModel::changeTreats,
            contentPadding = contentPadding,
        )
    }
}
