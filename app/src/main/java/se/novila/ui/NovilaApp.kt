package se.novila.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.PermissionController
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
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.nutrition.NutritionScreen
import se.novila.feature.sleep.SleepScreen
import se.novila.feature.today.TodayScreen
import se.novila.feature.training.TrainingScreen
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.MOVEMENT_PERMISSIONS
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
    val movement by vm.movement.collectAsStateWithLifecycle()

    // Health Connect's own permission dialog. It links to HealthDataInfoActivity for the details.
    val context = LocalContext.current
    val healthPermissions = rememberLauncherForActivityResult(
        remember { PermissionController.createRequestPermissionResultContract() },
        vm::onMovementPermissionsResult,
    )
    val current = TopLevelDestination.of(navigationState.topLevelRoute)

    NovilaTheme {
        NovilaScaffold(
            // Today is a dashboard with its own greeting header instead of a title bar.
            topBar = {
                if (current != TopLevelDestination.Today) NovilaTopAppBar(title = stringResource(current.label))
            },
            bottomBar = {
                NovilaNavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NovilaNavigationBarItem(
                            selected = destination == current,
                            onClick = { navigator.navigate(destination.route) },
                            icon = painterResource(destination.icon),
                            label = stringResource(destination.label),
                            role = destination.role(),
                        )
                    }
                }
            },
        ) { innerPadding ->
            val entryProvider = entryProvider<NavKey> {
                entry<TodayRoute> {
                    TodayScreen(
                        today = state.today,
                        history = state.history,
                        hour = state.hour,
                        movement = movement,
                        onMood = vm::setMood,
                        onOpenTraining = { navigator.navigate(TrainingRoute) },
                        onOpenSleep = { navigator.navigate(SleepRoute) },
                        onOpenNutrition = { navigator.navigate(NutritionRoute) },
                        contentPadding = innerPadding,
                    )
                }
                entry<TrainingRoute> {
                    TrainingScreen(
                        movement = movement,
                        onConnectHealth = { healthPermissions.launch(MOVEMENT_PERMISSIONS) },
                        onOpenHealthSettings = { HealthConnectSource.openSettings(context) },
                        onInstallHealth = { HealthConnectSource.openInstall(context) },
                        contentPadding = innerPadding,
                    )
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

/** Each tab shows its part's color. Today has no part of its own and uses the brand pine. */
@Composable
private fun TopLevelDestination.role(): NovilaColorRole = when (this) {
    TopLevelDestination.Today, TopLevelDestination.Training -> NovilaTheme.pillars.training
    TopLevelDestination.Sleep -> NovilaTheme.pillars.sleep
    TopLevelDestination.Nutrition -> NovilaTheme.pillars.nutrition
}
