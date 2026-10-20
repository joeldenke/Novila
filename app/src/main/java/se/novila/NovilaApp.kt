package se.novila

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import se.novila.R
import se.novila.designsystem.component.NovilaIconButton
import se.novila.designsystem.component.NovilaNavigationBar
import se.novila.designsystem.component.NovilaNavigationBarItem
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaTopAppBar
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.nutrition.navigation.NutritionRoute
import se.novila.feature.nutrition.navigation.nutritionEntry
import se.novila.feature.onboarding.navigation.OnboardingRoute
import se.novila.feature.onboarding.navigation.onboardingEntry
import se.novila.feature.settings.navigation.FoodPreferencesRoute
import se.novila.feature.settings.navigation.SettingsRoute
import se.novila.feature.settings.navigation.settingsEntries
import se.novila.feature.settings.navigation.settingsTitle
import se.novila.feature.sleep.navigation.SleepCoachRoute
import se.novila.feature.sleep.navigation.SleepRoute
import se.novila.feature.sleep.navigation.sleepDetailTitle
import se.novila.feature.sleep.navigation.sleepEntries
import se.novila.feature.today.navigation.todayEntry
import se.novila.feature.training.navigation.CoachRoute
import se.novila.feature.training.navigation.TrainingRoute
import se.novila.feature.training.navigation.trainingDetailTitle
import se.novila.feature.training.navigation.trainingEntries
import se.novila.navigation.Navigator
import se.novila.navigation.TopLevelDestination
import se.novila.navigation.rememberNavigationState
import se.novila.navigation.rememberStandaloneEntries
import se.novila.navigation.toEntries
import se.novila.ui.R as UiR

/**
 * The app shell: tabs, top bar and the navigation display. Each feature module brings its own routes and entries
 * (`todayEntry`, `trainingEntries`, ...), and each entry its own ViewModel from Metro, scoped to the entry. This only
 * connects the tabs to each other; it holds no screen state.
 *
 * Settings opens from the top bar (on Today, from its header) and goes onto the current tab's back stack.
 *
 * Until the intro is done ([onboarded]), the app shows only the intro, from `:feature:onboarding`.
 *
 * @param darkTheme and [dynamicColor] come from the user's settings, read in [MainActivity], as does [onboarded]
 */
@Composable
fun NovilaApp(darkTheme: Boolean, dynamicColor: Boolean, onboarded: Boolean) {
    NovilaTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
        if (onboarded) Tabs() else Intro()
    }
}

/**
 * The intro on its own, without tabs or top bar, until it is done. Finishing it saves the flag that [MainActivity]
 * reads, and the tabs take over.
 */
@Composable
private fun Intro() {
    val entries = rememberStandaloneEntries(OnboardingRoute, entryProvider<NavKey> { onboardingEntry() })
    entries.last().Content()
}

@Composable
private fun Tabs() {
    val navigationState = rememberNavigationState(
        startRoute = TopLevelDestination.start.route,
        topLevelRoutes = TopLevelDestination.routes,
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val current = TopLevelDestination.of(navigationState.topLevelRoute)
    val currentRoute = navigationState.backStacks[navigationState.topLevelRoute]?.lastOrNull()
    val openSettings = { navigator.navigate(SettingsRoute) }

    NovilaScaffold(
        // Today is a dashboard with its own greeting header instead of a title bar.
        topBar = {
            val detailTitle =
                trainingDetailTitle(currentRoute) ?: sleepDetailTitle(currentRoute) ?: settingsTitle(currentRoute)
            when {
                detailTitle != null -> NovilaTopAppBar(
                    title = detailTitle,
                    navigationIcon = painterResource(R.drawable.ic_back),
                    navigationLabel = stringResource(R.string.navigate_back),
                    onNavigate = navigator::goBack,
                )

                current != TopLevelDestination.Today -> NovilaTopAppBar(
                    title = stringResource(current.label),
                    actions = {
                        NovilaIconButton(
                            painter = painterResource(UiR.drawable.ic_settings),
                            contentDescription = stringResource(UiR.string.settings_title),
                            onClick = openSettings,
                        )
                    },
                )
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
                onOpenSettings = openSettings,
                contentPadding = innerPadding,
            )
            trainingEntries(navigator = navigator, contentPadding = innerPadding)
            sleepEntries(navigator = navigator, contentPadding = innerPadding)
            nutritionEntry(
                onOpenFoodPreferences = { navigator.navigate(FoodPreferencesRoute) },
                contentPadding = innerPadding,
            )
            settingsEntries(
                navigator = navigator,
                contentPadding = innerPadding,
                // My goals opens the coaches in place, on the tab Settings is on.
                onOpenTrainingCoach = { navigator.navigate(CoachRoute) },
                onOpenSleepCoach = { navigator.navigate(SleepCoachRoute) },
                // Generated by the AboutLibraries plugin from everything the app ships.
                licensesResId = R.raw.aboutlibraries,
            )
        }
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            // Popping an entry clears its ViewModel: leaving a session without finishing it discards it.
            onBack = navigator::goBack,
        )
    }
}

/** Each tab shows its part's color. Today has no part of its own and uses the brand pine. */
@Composable
private fun TopLevelDestination.role(): NovilaColorRole = when (this) {
    TopLevelDestination.Today, TopLevelDestination.Training -> NovilaTheme.pillars.training
    TopLevelDestination.Sleep -> NovilaTheme.pillars.sleep
    TopLevelDestination.Nutrition -> NovilaTheme.pillars.nutrition
}
