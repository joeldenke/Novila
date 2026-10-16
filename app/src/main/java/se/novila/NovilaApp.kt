package se.novila

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
import se.novila.feature.training.navigation.TrainingState
import se.novila.feature.training.navigation.WorkoutRoute
import se.novila.feature.training.navigation.trainingDetailTitle
import se.novila.feature.training.navigation.trainingEntries
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.MOVEMENT_PERMISSIONS
import se.novila.navigation.Navigator
import se.novila.navigation.TopLevelDestination
import se.novila.navigation.rememberNavigationState
import se.novila.navigation.toEntries

/**
 * The app shell: tabs, top bar and the navigation display. Each feature module brings its own routes and entries
 * (`todayEntry`, `trainingEntries`, ...); this only connects them to [HealthViewModel] and to each other.
 */
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
    val workout by vm.workout.collectAsStateWithLifecycle()
    val assistant by vm.assistant.collectAsStateWithLifecycle()

    // Health Connect's own permission dialog. It links to HealthDataInfoActivity for the details.
    val context = LocalContext.current
    val healthPermissions = rememberLauncherForActivityResult(
        remember { PermissionController.createRequestPermissionResultContract() },
        vm::onMovementPermissionsResult,
    )
    val current = TopLevelDestination.of(navigationState.topLevelRoute)
    val currentRoute = navigationState.backStacks[navigationState.topLevelRoute]?.lastOrNull()

    // Leaving a session without finishing it discards it; nothing was saved yet.
    val goBack = {
        if (currentRoute is WorkoutRoute) vm.stopWorkout()
        navigator.goBack()
    }

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
                        onNavigate = goBack,
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
                trainingEntries(
                    state = TrainingState(
                        movement = movement,
                        program = state.program,
                        coach = state.coach,
                        assistant = assistant,
                        workout = workout,
                        today = state.today.date.dayOfWeek,
                        strengthMinutesToday = state.today.strengthMinutes,
                    ),
                    navigator = navigator,
                    contentPadding = innerPadding,
                    onConnectHealth = { healthPermissions.launch(MOVEMENT_PERMISSIONS) },
                    onOpenHealthSettings = { HealthConnectSource.openSettings(context) },
                    onInstallHealth = { HealthConnectSource.openInstall(context) },
                    onCheckAssistant = vm::checkAssistant,
                    onDownloadAssistant = vm::downloadAssistant,
                    onWriteAssistantMessage = vm::writeAssistantMessage,
                    onChangeCoach = vm::changeCoach,
                    onCreateCoachProgram = vm::createCoachProgram,
                    onNextCoachSuggestion = vm::nextCoachSuggestion,
                    onResetCoach = vm::resetCoach,
                    onStartWorkout = vm::startWorkout,
                    onNextWorkoutStep = vm::nextWorkoutStep,
                    onFinishWorkout = vm::finishWorkout,
                )
                sleepEntry(state.today, state.history, vm::setSleep, innerPadding)
                nutritionEntry(state.today, state.history, vm::changePortions, vm::changeTreats, innerPadding)
            }
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack = goBack,
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
