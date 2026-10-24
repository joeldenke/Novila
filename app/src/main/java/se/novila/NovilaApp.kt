package se.novila

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import se.novila.R
import se.novila.designsystem.component.LocalNovilaFold
import se.novila.designsystem.component.LocalNovilaSharedTransitionScope
import se.novila.designsystem.component.NovilaEntryLayout
import se.novila.designsystem.component.NovilaIconButton
import se.novila.designsystem.component.NovilaNavigationBar
import se.novila.designsystem.component.NovilaNavigationBarItem
import se.novila.designsystem.component.NovilaNavigationRail
import se.novila.designsystem.component.NovilaNavigationRailItem
import se.novila.designsystem.component.NovilaRailMinWidth
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaTopAppBar
import se.novila.designsystem.component.NovilaTransitions
import se.novila.designsystem.component.rememberNovilaFold
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.breathing.navigation.BreathingRoute
import se.novila.feature.breathing.navigation.breathingDetailTitle
import se.novila.feature.breathing.navigation.breathingEntries
import se.novila.feature.nutrition.navigation.NutritionRoute
import se.novila.feature.nutrition.navigation.nutritionDetailTitle
import se.novila.feature.nutrition.navigation.nutritionEntries
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
import se.novila.feature.today.navigation.todayDetailTitle
import se.novila.feature.today.navigation.todayEntries
import se.novila.feature.training.navigation.CoachRoute
import se.novila.feature.training.navigation.TrainingRoute
import se.novila.feature.training.navigation.WorkoutRoute
import se.novila.feature.training.navigation.trainingDetailTitle
import se.novila.feature.training.navigation.trainingEntries
import se.novila.model.EntryLayout
import se.novila.navigation.NavigationState
import se.novila.navigation.Navigator
import se.novila.navigation.TopLevelDestination
import se.novila.navigation.rememberNavigationState
import se.novila.navigation.rememberStandaloneEntries
import se.novila.navigation.toEntries
import java.time.LocalDate
import se.novila.ui.R as UiR

/**
 * The app shell: tabs, top bar and the navigation display. Each feature module brings its own routes and entries
 * (`todayEntries`, `trainingEntries`, ...), and each entry its own ViewModel from Metro, scoped to the entry. This only
 * connects the tabs to each other; it holds no screen state.
 *
 * Settings opens from the top bar (on Today, from its header) and goes onto the current tab's back stack.
 *
 * Until the intro is done ([onboarded]), the app shows only the intro, from `:feature:onboarding`.
 *
 * @param darkTheme and [dynamicColor] come from the user's settings, read in [MainActivity], as do [onboarded] and
 *   [entryLayout] (tiles or rows for the ways into each tab's parts)
 */
@Composable
fun NovilaApp(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    onboarded: Boolean,
    entryLayout: EntryLayout,
    shortcut: AppShortcut? = null,
    onShortcutShown: () -> Unit = {},
) {
    val layout = when (entryLayout) {
        EntryLayout.Grid -> NovilaEntryLayout.Grid
        EntryLayout.Rows -> NovilaEntryLayout.Rows
    }
    NovilaTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, entryLayout = layout) {
        // On an unfolded phone, two-pane screens split along the fold.
        CompositionLocalProvider(LocalNovilaFold provides rememberNovilaFold()) {
            if (onboarded) {
                Tabs(shortcut, onShortcutShown)
            } else {
                // The intro comes first; a shortcut tapped before it is done leads nowhere afterwards.
                LaunchedEffect(shortcut) { if (shortcut != null) onShortcutShown() }
                Intro()
            }
        }
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
private fun Tabs(shortcut: AppShortcut?, onShortcutShown: () -> Unit) {
    val navigationState = rememberNavigationState(
        startRoute = TopLevelDestination.start.route,
        topLevelRoutes = TopLevelDestination.routes,
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    // A launcher shortcut opens where it leads: today's session in session mode, or Nutrition with the glass logged.
    LaunchedEffect(shortcut) {
        when (shortcut) {
            AppShortcut.StartSession -> {
                navigator.navigate(TrainingRoute)
                val workout = WorkoutRoute(LocalDate.now().dayOfWeek)
                if (navigationState.backStacks[TrainingRoute]?.contains(workout) != true) navigator.navigate(workout)
            }

            AppShortcut.LogWater -> navigator.navigate(NutritionRoute)

            null -> return@LaunchedEffect
        }
        onShortcutShown()
    }
    val current = TopLevelDestination.of(navigationState.topLevelRoute)
    val currentRoute = navigationState.backStacks[navigationState.topLevelRoute]?.lastOrNull()
    val openSettings = { navigator.navigate(SettingsRoute) }

    // A wide window (tablet, unfolded phone, landscape) has the tabs in a rail at the side instead of a bar below.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= NovilaRailMinWidth
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                NovilaNavigationRail {
                    TopLevelDestination.entries.forEach { destination ->
                        NovilaNavigationRailItem(
                            selected = destination == current,
                            onClick = { navigator.navigate(destination.route) },
                            icon = painterResource(destination.icon),
                            label = stringResource(destination.label),
                            role = destination.role(),
                        )
                    }
                }
            }
            NovilaScaffold(
                // The rail already keeps clear of the start inset, so the content doesn't add it again.
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (wide) {
                            Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                        } else {
                            Modifier
                        },
                    ),
                // Today is a dashboard with its own greeting header instead of a title bar.
                topBar = {
                    val detailTitle = todayDetailTitle(currentRoute)
                        ?: trainingDetailTitle(currentRoute)
                        ?: sleepDetailTitle(currentRoute)
                        ?: breathingDetailTitle(currentRoute)
                        ?: nutritionDetailTitle(currentRoute)
                        ?: settingsTitle(currentRoute)
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
                    if (!wide) {
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
                    }
                },
            ) { innerPadding ->
                TabContent(navigator, navigationState, openSettings, innerPadding)
            }
        }
    }
}

/** The current tab's back stack, with every feature's entries. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TabContent(
    navigator: Navigator,
    navigationState: NavigationState,
    openSettings: () -> Unit,
    innerPadding: PaddingValues,
) {
    val entryProvider = entryProvider<NavKey> {
        todayEntries(
            navigator = navigator,
            onOpenTraining = { navigator.navigate(TrainingRoute) },
            onOpenSleep = { navigator.navigate(SleepRoute) },
            onOpenNutrition = { navigator.navigate(NutritionRoute) },
            onOpenSettings = openSettings,
            contentPadding = innerPadding,
        )
        trainingEntries(navigator = navigator, contentPadding = innerPadding)
        // The breathing sessions open from Sleep's plan, alongside the sleep coach's evening routine.
        sleepEntries(
            navigator = navigator,
            contentPadding = innerPadding,
            onOpenBreathing = { navigator.navigate(BreathingRoute) },
        )
        breathingEntries(navigator = navigator, contentPadding = innerPadding)
        nutritionEntries(
            navigator = navigator,
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
    // Tiles grow into the screens they open (NovilaSharedBounds); the moves between screens are Novila's own: deeper
    // and back along one axis, a fade between tabs, and the screen below showing through while dragging back.
    SharedTransitionLayout(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalNovilaSharedTransitionScope provides this) {
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                transitionSpec = {
                    if (navigationState.switchedTab) NovilaTransitions.tab else NovilaTransitions.forward
                },
                popTransitionSpec = {
                    if (navigationState.switchedTab) NovilaTransitions.tab else NovilaTransitions.back
                },
                predictivePopTransitionSpec = { NovilaTransitions.predictiveBack },
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
