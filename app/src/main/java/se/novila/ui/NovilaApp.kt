package se.novila.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import se.novila.HealthViewModel
import se.novila.designsystem.component.NovilaNavigationBar
import se.novila.designsystem.component.NovilaNavigationBarItem
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaTopAppBar
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.nutrition.NutritionScreen
import se.novila.feature.sleep.SleepScreen
import se.novila.feature.today.TodayScreen
import se.novila.feature.training.TrainingScreen
import se.novila.navigation.Navigator
import se.novila.navigation.NutritionRoute
import se.novila.navigation.SleepRoute
import se.novila.navigation.TodayRoute
import se.novila.navigation.TopLevelDestination
import se.novila.navigation.TrainingRoute
import se.novila.navigation.rememberNavigationState
import se.novila.navigation.toEntries

@Composable
fun NovilaApp(vm: HealthViewModel = viewModel()) {
    // Re-read data on resume so "today" is correct after midnight.
    LifecycleResumeEffect(vm) {
        vm.refresh()
        onPauseOrDispose { }
    }

    val navigationState = rememberNavigationState(
        startRoute = TopLevelDestination.start.route,
        topLevelRoutes = TopLevelDestination.routes,
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val state by vm.state.collectAsStateWithLifecycle()
    val current = TopLevelDestination.of(navigationState.topLevelRoute)

    NovilaTheme {
        NovilaScaffold(
            topBar = { NovilaTopAppBar(title = stringResource(current.label)) },
            bottomBar = {
                NovilaNavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NovilaNavigationBarItem(
                            selected = destination == current,
                            onClick = { navigator.navigate(destination.route) },
                            icon = painterResource(destination.icon),
                            label = stringResource(destination.label),
                        )
                    }
                }
            },
        ) { innerPadding ->
            val entryProvider = entryProvider<NavKey> {
                entry<TodayRoute> {
                    TodayScreen(state.today, state.history, vm::setMood, innerPadding)
                }
                entry<TrainingRoute> {
                    TrainingScreen(innerPadding)
                }
                entry<SleepRoute> {
                    SleepScreen(state.today, state.history, vm::setSleep, innerPadding)
                }
                entry<NutritionRoute> {
                    NutritionScreen(state.today, state.history, vm::addWater, vm::removeWater, innerPadding)
                }
            }
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack = { navigator.goBack() },
            )
        }
    }
}
