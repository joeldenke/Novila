package se.novila.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import se.novila.designsystem.component.LocalNovilaAnimatedVisibilityScope
import se.novila.designsystem.component.NovilaSharedBounds

/**
 * The screens that open from a tile on a tab (`NovilaEntry.sharedKey`) and grow out of it. A tile and its screen
 * can live in different features (Sleep opens Breathing), so the keys they share are kept here, where every feature
 * can reach them, instead of in either feature.
 */
enum class SharedScreen {
    TrainingProgram,
    TrainingCoach,
    ExerciseLibrary,
    SleepCoach,
    Breathing,
    SleepTrends,
    Recipes,
    NutritionTrends,
    FoodPreferences,
}

/**
 * The metadata that makes an entry grow out of the tile with the same [screen] key:
 * `entry<ProgramRoute>(metadata = sharedScreen(SharedScreen.TrainingProgram)) { … }`. Opened another way (from
 * My goals, a notification), there is no tile and the screen moves in like any other.
 */
fun sharedScreen(screen: SharedScreen): Map<String, Any> = mapOf(SHARED_SCREEN_KEY to screen)

private const val SHARED_SCREEN_KEY = "se.novila.navigation.SharedScreen"

/**
 * Gives every entry in `NavDisplay` its enter and exit for [NovilaSharedBounds], and puts the screens marked with
 * [sharedScreen] in one, so the design system can move a tile into them without knowing about navigation.
 */
internal val sharedScreenDecorator = NavEntryDecorator<NavKey> { entry ->
    CompositionLocalProvider(LocalNovilaAnimatedVisibilityScope provides LocalNavAnimatedContentScope.current) {
        val key = entry.metadata[SHARED_SCREEN_KEY]
        if (key == null) {
            entry.Content()
        } else {
            // The screen fills the display as before (the inner box gives it the usual loose constraints), and is only
            // clipped to the tile's shape on the way.
            NovilaSharedBounds(key, Modifier.fillMaxSize()) { Box { entry.Content() } }
        }
    }
}
