package se.novila.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import se.novila.R
import se.novila.anatomy.ExerciseLibrary
import se.novila.designsystem.component.NovilaNavigationBar
import se.novila.designsystem.component.NovilaNavigationBarItem
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.component.NovilaTopAppBar
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.nutrition.NutritionScreen
import se.novila.feature.sleep.SleepScreen
import se.novila.feature.today.TodayScreen
import se.novila.feature.training.CoachScreen
import se.novila.feature.training.ExerciseDetailScreen
import se.novila.feature.training.TrainingScreen
import se.novila.feature.training.WorkoutScreen
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.MOVEMENT_PERMISSIONS
import se.novila.navigation.CoachRoute
import se.novila.navigation.ExerciseRoute
import se.novila.navigation.Navigator
import se.novila.navigation.NutritionRoute
import se.novila.navigation.SleepRoute
import se.novila.navigation.TodayRoute
import se.novila.navigation.TopLevelDestination
import se.novila.navigation.TrainingRoute
import se.novila.navigation.WorkoutRoute
import se.novila.navigation.rememberNavigationState
import se.novila.navigation.toEntries
import se.novila.ui.common.moveName

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

    NovilaTheme {
        NovilaScaffold(
            // Today is a dashboard with its own greeting header instead of a title bar.
            topBar = {
                when {
                    currentRoute is ExerciseRoute -> NovilaTopAppBar(
                        title = moveName(currentRoute.exerciseId),
                        navigationIcon = painterResource(R.drawable.ic_back),
                        navigationLabel = stringResource(R.string.navigate_back),
                        onNavigate = navigator::goBack,
                    )

                    currentRoute is CoachRoute -> NovilaTopAppBar(
                        title = stringResource(R.string.coach_title),
                        navigationIcon = painterResource(R.drawable.ic_back),
                        navigationLabel = stringResource(R.string.navigate_back),
                        onNavigate = navigator::goBack,
                    )

                    currentRoute is WorkoutRoute -> NovilaTopAppBar(
                        title = stringResource(R.string.workout_title),
                        navigationIcon = painterResource(R.drawable.ic_back),
                        navigationLabel = stringResource(R.string.navigate_back),
                        onNavigate = {
                            vm.stopWorkout()
                            navigator.goBack()
                        },
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
                        program = state.program,
                        coach = state.coach,
                        today = state.today.date.dayOfWeek,
                        onConnectHealth = { healthPermissions.launch(MOVEMENT_PERMISSIONS) },
                        onOpenHealthSettings = { HealthConnectSource.openSettings(context) },
                        onInstallHealth = { HealthConnectSource.openInstall(context) },
                        onOpenExercise = { navigator.navigate(ExerciseRoute(it)) },
                        onStartWorkout = { day ->
                            vm.startWorkout(day)
                            navigator.navigate(WorkoutRoute)
                        },
                        onOpenCoach = { navigator.navigate(CoachRoute) },
                        contentPadding = innerPadding,
                        strengthMinutesToday = state.today.strengthMinutes,
                    )
                }
                entry<CoachRoute> {
                    // Whether Gemini Nano can run here is asked when the coach opens, never in the background.
                    LaunchedEffect(Unit) { vm.checkAssistant() }
                    CoachScreen(
                        coach = state.coach,
                        assistant = assistant,
                        today = state.today.date.dayOfWeek,
                        onChange = vm::changeCoach,
                        onCreate = vm::createCoachProgram,
                        onSuggest = vm::nextCoachSuggestion,
                        onReset = vm::resetCoach,
                        onOpenExercise = { navigator.navigate(ExerciseRoute(it)) },
                        onDownloadAssistant = vm::downloadAssistant,
                        onWriteMessage = vm::writeAssistantMessage,
                        contentPadding = innerPadding,
                    )
                }
                entry<WorkoutRoute> {
                    WorkoutScreen(
                        state = workout,
                        onNext = vm::nextWorkoutStep,
                        onFinish = {
                            vm.finishWorkout()
                            navigator.goBack()
                        },
                        onShowHow = { navigator.navigate(ExerciseRoute(it)) },
                        contentPadding = innerPadding,
                    )
                }
                entry<ExerciseRoute> { route ->
                    ExerciseLibrary.byId(route.exerciseId)?.let { motion ->
                        ExerciseDetailScreen(
                            motion = motion,
                            planned = state.program.sessions.flatMap { it.exercises }
                                .firstOrNull { it.exerciseId == route.exerciseId },
                            contentPadding = innerPadding,
                        )
                    }
                }
                entry<SleepRoute> {
                    SleepScreen(state.today, state.history, vm::setSleep, innerPadding)
                }
                entry<NutritionRoute> {
                    NutritionScreen(state.today, state.history, vm::changePortions, vm::changeTreats, innerPadding)
                }
            }
            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack = {
                    // Leaving a session without finishing it discards it; nothing was saved yet.
                    if (currentRoute is WorkoutRoute) vm.stopWorkout()
                    navigator.goBack()
                },
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
