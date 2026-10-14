package se.novila.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import se.novila.R

@Serializable data object TodayRoute : NavKey
@Serializable data object TrainingRoute : NavKey
@Serializable data object SleepRoute : NavKey
@Serializable data object NutritionRoute : NavKey

/** Tabs in the bottom navigation bar, in display order. Each tab keeps its own back stack. */
enum class TopLevelDestination(
    val route: NavKey,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
) {
    Today(TodayRoute, R.string.nav_today, R.drawable.ic_today),
    Training(TrainingRoute, R.string.nav_training, R.drawable.ic_training),
    Sleep(SleepRoute, R.string.nav_sleep, R.drawable.ic_sleep),
    Nutrition(NutritionRoute, R.string.nav_nutrition, R.drawable.ic_nutrition),
    ;

    companion object {
        val start = Today
        val routes: Set<NavKey> = entries.map { it.route }.toSet()

        fun of(route: NavKey): TopLevelDestination = entries.first { it.route == route }
    }
}
