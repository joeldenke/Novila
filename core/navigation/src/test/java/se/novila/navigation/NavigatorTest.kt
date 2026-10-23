package se.novila.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tab navigation rules Novila relies on: one back stack per tab, and a screen opened from any tab (such as
 * Settings) living in exactly one of them at a time. Plain JVM: [Navigator] is pure logic over [NavigationState].
 */
class NavigatorTest {
    private data object Idag : NavKey
    private data object Training : NavKey
    private data object Settings : NavKey
    private data object Reminders : NavKey

    private fun navigator(): Navigator {
        val state = NavigationState(
            startRoute = Idag,
            topLevelRoute = mutableStateOf(Idag),
            backStacks = mapOf(Idag to NavBackStack(Idag), Training to NavBackStack(Training)),
        )
        return Navigator(state)
    }

    @Test fun `a top-level route switches the tab and keeps that tab's history`() {
        val navigator = navigator()
        navigator.navigate(Training)
        navigator.navigate(Settings)
        navigator.navigate(Idag)
        assertEquals(Idag, navigator.state.topLevelRoute)
        navigator.navigate(Training)
        assertEquals(Training, navigator.state.topLevelRoute)
        assertEquals(listOf(Training, Settings), navigator.state.backStacks[Training]?.toList())
    }

    @Test fun `a screen opened from another tab moves to this tab and leaves the old stack`() {
        val navigator = navigator()
        navigator.navigate(Settings)
        navigator.navigate(Reminders)
        navigator.state.topLevelRoute = Training
        navigator.navigate(Settings)
        assertEquals(listOf(Training, Settings), navigator.state.backStacks[Training]?.toList())
        // Popped from the old stack, so the same key is never in two stacks shown at once.
        assertEquals(listOf(Idag), navigator.state.backStacks[Idag]?.toList())
    }

    @Test fun `a screen opened twice in a row is opened once`() {
        val navigator = navigator()
        navigator.navigate(Settings)
        navigator.navigate(Settings)
        assertEquals(listOf(Idag, Settings), navigator.state.backStacks[Idag]?.toList())
    }

    @Test fun `a screen already open further back in the tab is gone back to`() {
        val navigator = navigator()
        navigator.navigate(Settings)
        navigator.navigate(Reminders)
        navigator.navigate(Settings)
        assertEquals(listOf(Idag, Settings), navigator.state.backStacks[Idag]?.toList())
    }

    @Test fun `goBack at a tab's root returns to the start tab instead of leaving the app`() {
        val navigator = navigator()
        navigator.navigate(Training)
        navigator.goBack()
        assertEquals(Idag, navigator.state.topLevelRoute)
    }

    @Test fun `goBack pops the screen on top of the tab's root`() {
        val navigator = navigator()
        navigator.navigate(Settings)
        navigator.goBack()
        assertEquals(listOf(Idag), navigator.state.backStacks[Idag]?.toList())
        assertEquals(Idag, navigator.state.topLevelRoute)
    }

    @Test fun `only a tap on another tab counts as switching tabs`() {
        val navigator = navigator()
        navigator.navigate(Training)
        assertTrue(navigator.state.switchedTab)
        navigator.navigate(Settings)
        assertFalse(navigator.state.switchedTab)
        navigator.navigate(Idag)
        assertTrue(navigator.state.switchedTab)
        navigator.navigate(Idag)
        assertFalse(navigator.state.switchedTab)
        navigator.navigate(Training)
        navigator.goBack()
        assertFalse(navigator.state.switchedTab)
    }

    @Test fun `stacks in use are the start tab plus the open one`() {
        val navigator = navigator()
        assertEquals(listOf(Idag), navigator.state.stacksInUse)
        navigator.navigate(Training)
        assertEquals(listOf(Idag, Training), navigator.state.stacksInUse)
        navigator.navigate(Idag)
        assertEquals(listOf(Idag), navigator.state.stacksInUse)
    }
}
