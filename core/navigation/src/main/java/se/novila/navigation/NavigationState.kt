package se.novila.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

// Multiple back stacks, following the Navigation 3 migration guide (android/skills: navigation-3).

/** Navigation state that survives configuration changes and process death. */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>,
): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute,
        topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer()),
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
        )
    }
}

/**
 * Holds one back stack per top-level route.
 *
 * @param startRoute the user exits the app from this route
 * @param topLevelRoute the currently selected top-level route
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
    var topLevelRoute: NavKey by topLevelRoute

    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}

/**
 * Flattens the stacks in use into decorated entries for `NavDisplay`.
 *
 * Every entry gets its own saved state and its own `ViewModelStore`, so `metroViewModel()` in an entry gives a
 * ViewModel scoped to that entry: it survives rotation and switching tabs, and is cleared when the entry is popped.
 * All stacks are decorated, not only the ones shown, so another tab's entries keep their ViewModels while that tab
 * is in the background.
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): SnapshotStateList<NavEntry<NavKey>> {
    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf<NavEntryDecorator<NavKey>>(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        )
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider,
        )
    }

    return stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
}

/**
 * A single [route] shown on its own, outside the tabs, such as the intro before the app. Its entry gets its own saved
 * state and `ViewModelStore` like every tab entry, so `metroViewModel()` works the same. Show it with
 * `entries.last().Content()`; there is nothing to go back to, so system back stays with the screen and the app.
 */
@Composable
fun rememberStandaloneEntries(
    route: NavKey,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): List<NavEntry<NavKey>> {
    val backStack = rememberNavBackStack(route)
    return rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf<NavEntryDecorator<NavKey>>(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider,
    )
}
