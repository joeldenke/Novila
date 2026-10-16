package se.novila.navigation

import androidx.navigation3.runtime.NavKey

/** Handles navigation events by updating [NavigationState] (unidirectional data flow). */
class Navigator(val state: NavigationState) {

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            // Top-level route: switch tab and keep that tab's history.
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        // At the root of a tab, go back to the start tab instead of leaving the app.
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}
