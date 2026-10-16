package se.novila.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import se.novila.R
import se.novila.feature.nutrition.navigation.NutritionRoute
import se.novila.feature.sleep.navigation.SleepRoute
import se.novila.feature.today.navigation.TodayRoute
import se.novila.feature.training.navigation.TrainingRoute
import se.novila.ui.R as UiR

/** Tabs in the bottom navigation bar, in display order. Each tab keeps its own back stack. */
enum class TopLevelDestination(
    val route: NavKey,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    Today(TodayRoute, UiR.string.nav_today, R.drawable.ic_today),
    Training(TrainingRoute, UiR.string.nav_training, UiR.drawable.ic_training),
    Sleep(SleepRoute, UiR.string.nav_sleep, UiR.drawable.ic_sleep),
    Nutrition(NutritionRoute, UiR.string.nav_nutrition, UiR.drawable.ic_nutrition),
    ;

    companion object {
        val start = Today
        val routes: Set<NavKey> = entries.map { it.route }.toSet()

        fun of(route: NavKey): TopLevelDestination = entries.first { it.route == route }
    }
}
