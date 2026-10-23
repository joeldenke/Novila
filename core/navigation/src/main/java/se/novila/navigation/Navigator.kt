package se.novila.navigation

import androidx.navigation3.runtime.NavKey

/** Handles navigation events by updating [NavigationState] (unidirectional data flow). */
class Navigator(val state: NavigationState) {

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            // Top-level route: switch tab and keep that tab's history.
            state.switchedTab = route != state.topLevelRoute
            state.topLevelRoute = route
        } else {
            state.switchedTab = false
            // A screen that opens from any tab (Settings) lives in one back stack at a time: the start tab's stack is
            // shown under the current one, and the same key twice in NavDisplay clashes. Opening it in another tab
            // moves it here, together with what was opened from it.
            state.backStacks.forEach { (topLevelRoute, stack) ->
                val index = stack.indexOf(route)
                if (topLevelRoute != state.topLevelRoute && index >= 0) {
                    while (stack.size > index) stack.removeLastOrNull()
                }
            }
            val stack = state.backStacks[state.topLevelRoute] ?: return
            // Already open in this tab (a second tap on a tile while its screen slides in): back to it instead of
            // opening it twice, since the same key twice in one back stack clashes in NavDisplay.
            val open = stack.indexOf(route)
            if (open >= 0) {
                while (stack.size > open + 1) stack.removeLastOrNull()
            } else {
                stack.add(route)
            }
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        // At the root of a tab, go back to the start tab instead of leaving the app.
        state.switchedTab = false
        if (currentRoute == state.topLevelRoute) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}
