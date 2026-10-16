package se.novila

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import se.novila.R
import se.novila.designsystem.component.NovilaNavigationBar
import se.novila.designsystem.component.NovilaNavigationBarItem
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaTopAppBar
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.nutrition.navigation.NutritionRoute
import se.novila.feature.nutrition.navigation.nutritionEntry
import se.novila.feature.sleep.navigation.SleepRoute
import se.novila.feature.sleep.navigation.sleepEntry
import se.novila.feature.today.navigation.todayEntry
import se.novila.feature.training.navigation.TrainingRoute
import se.novila.feature.training.navigation.trainingDetailTitle
import se.novila.feature.training.navigation.trainingEntries
import se.novila.navigation.Navigator
import se.novila.navigation.TopLevelDestination
import se.novila.navigation.rememberNavigationState
import se.novila.navigation.toEntries

/**
 * The app shell: tabs, top bar and the navigation display. Each feature module brings its own routes and entries
 * (`todayEntry`, `trainingEntries`, ...), and each entry its own ViewModel from Metro, scoped to the entry. This only
 * connects the tabs to each other; it holds no screen state.
 */
@Composable
fun NovilaApp() {
    val navigationState = rememberNavigationState(
        startRoute = TopLevelDestination.start.route,
        topLevelRoutes = TopLevelDestination.routes,
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val current = TopLevelDestination.of(navigationState.topLevelRoute)
    val currentRoute = navigationState.backStacks[navigationState.topLevelRoute]?.lastOrNull()

    NovilaTheme {
        NovilaScaffold(
            // Today is a dashboard with its own greeting header instead of a title bar.
            topBar = {
                val detailTitle = trainingDetailTitle(currentRoute)
                when {
                    detailTitle != null -> NovilaTopAppBar(
                        title = detailTitle,
                        navigationIcon = painterResource(R.drawable.ic_back),
                        navigationLabel = stringResource(R.string.navigate_back),
                        onNavigate = navigator::goBack,
                    )

                    current != TopLevelDestination.Today -> NovilaTopAppBar(title = stringResource(current.label))
                }
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
                todayEntry(
                    onOpenTraining = { navigator.navigate(TrainingRoute) },
                    onOpenSleep = { navigator.navigate(SleepRoute) },
                    onOpenNutrition = { navigator.navigate(NutritionRoute) },
                    contentPadding = innerPadding,
                )
                trainingEntries(navigator = navigator, contentPadding = innerPadding)
                sleepEntry(contentPadding = innerPadding)
                nutritionEntry(contentPadding = innerPadding)
            }
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                // Popping an entry clears its ViewModel: leaving a session without finishing it discards it.
                onBack = navigator::goBack,
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
