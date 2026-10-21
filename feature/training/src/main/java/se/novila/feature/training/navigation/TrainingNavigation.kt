package se.novila.feature.training.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.anatomy.ExerciseLibrary
import se.novila.designsystem.component.NovilaSky
import se.novila.feature.training.CardioLogScreen
import se.novila.feature.training.CardioLogViewModel
import se.novila.feature.training.CoachScreen
import se.novila.feature.training.CoachViewModel
import se.novila.feature.training.ExerciseDetailScreen
import se.novila.feature.training.ExerciseLibraryScreen
import se.novila.feature.training.ExerciseViewModel
import se.novila.feature.training.R
import se.novila.feature.training.SessionScreen
import se.novila.feature.training.SessionViewModel
import se.novila.feature.training.TrainingActions
import se.novila.feature.training.TrainingScreen
import se.novila.feature.training.TrainingViewModel
import se.novila.feature.training.WorkoutScreen
import se.novila.feature.training.WorkoutViewModel
import se.novila.feature.training.moveName
import se.novila.feature.training.sessionTitle
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.RECOVERY_PERMISSIONS
import se.novila.healthconnect.rememberHealthPermissionRequest
import se.novila.navigation.Navigator
import java.time.DayOfWeek

/** The Training tab: movement, the week's program, the coach and the exercise library. */
@Serializable data object TrainingRoute : NavKey

/** How to do one exercise from the anatomy library. */
@Serializable data class ExerciseRoute(val exerciseId: String) : NavKey

/** The training coach: answers in, a week's program out. */
@Serializable data object CoachRoute : NavKey

/** Logging a workout by hand, planned or not. */
@Serializable data object CardioLogRoute : NavKey

/** Every exercise, with search. */
@Serializable data object ExerciseLibraryRoute : NavKey

/** One strength or rehab day of this week's program, before starting it ([day] is ISO, Monday is 1). */
@Serializable
data class SessionRoute(val day: Int) : NavKey {
    constructor(day: DayOfWeek) : this(day.value)

    val dayOfWeek: DayOfWeek get() = DayOfWeek.of(day)
}

/** A guided strength session, set by set, for the program's strength day [day] (ISO, Monday is 1). */
@Serializable
data class WorkoutRoute(val day: Int) : NavKey {
    constructor(day: DayOfWeek) : this(day.value)

    val dayOfWeek: DayOfWeek get() = DayOfWeek.of(day)
}

/**
 * The Training tab and the screens opened from it. Each entry gets its own ViewModel from Metro, scoped to the entry
 * (`metroViewModel()`), so it lives as long as the entry is on the back stack. Navigation inside the tab goes through
 * [navigator].
 */
fun EntryProviderScope<NavKey>.trainingEntries(navigator: Navigator, contentPadding: PaddingValues) {
    val openExercise = { id: String -> navigator.navigate(ExerciseRoute(id)) }
    entry<TrainingRoute> {
        val viewModel = metroViewModel<TrainingViewModel>()
        // Read again on resume so "today" is correct after midnight, and movement is fresh.
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        // Nothing to show until the saved days and answers are read, a moment at app start.
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        val movement by viewModel.movement.collectAsStateWithLifecycle()
        val exercise by viewModel.exercise.collectAsStateWithLifecycle()
        val recovery by viewModel.recovery.collectAsStateWithLifecycle()
        val hour by viewModel.hour.collectAsStateWithLifecycle()
        val setupIncomplete = viewModel.setupIncomplete.collectAsStateWithLifecycle().value
        val context = LocalContext.current
        val connectHealth = rememberHealthPermissionRequest(viewModel::onHealthPermissionsResult)
        // Recovery's own dialog asks only for resting heart rate and HRV, so it can be connected on its own.
        val connectRecovery = rememberHealthPermissionRequest(
            viewModel::onRecoveryPermissionsResult,
            permissions = RECOVERY_PERMISSIONS,
        )
        TrainingScreen(
            state = state,
            movement = movement,
            actions = TrainingActions(
                onStartWorkout = { day -> navigator.navigate(WorkoutRoute(day)) },
                onLogCardio = viewModel::logCardio,
                onOpenSession = { day -> navigator.navigate(SessionRoute(day)) },
                onOpenLibrary = { navigator.navigate(ExerciseLibraryRoute) },
                onOpenCardioLog = { navigator.navigate(CardioLogRoute) },
                onOpenCoach = { navigator.navigate(CoachRoute) },
                onOpenExercise = openExercise,
                onConnectHealth = connectHealth,
                onConnectRecovery = connectRecovery,
                onOpenHealthSettings = { HealthConnectSource.openSettings(context) },
                onInstallHealth = { HealthConnectSource.openInstall(context) },
                onCompleteSetup = viewModel::openSetup,
            ),
            contentPadding = contentPadding,
            exercise = exercise,
            recovery = recovery,
            sky = NovilaSky.at(hour),
            setupIncomplete = setupIncomplete,
        )
    }
    entry<SessionRoute> { route ->
        val viewModel = assistedMetroViewModel<SessionViewModel, SessionViewModel.Factory> {
            create(route.dayOfWeek)
        }
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        SessionScreen(
            state = state,
            onStart = { navigator.navigate(WorkoutRoute(route.dayOfWeek)) },
            onOpenExercise = openExercise,
            contentPadding = contentPadding,
        )
    }
    entry<ExerciseLibraryRoute> {
        ExerciseLibraryScreen(onOpenExercise = openExercise, contentPadding = contentPadding)
    }
    entry<CardioLogRoute> {
        val viewModel = metroViewModel<CardioLogViewModel>()
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        CardioLogScreen(state, onLog = viewModel::log, onUndo = viewModel::undo, contentPadding = contentPadding)
    }
    entry<CoachRoute> {
        val viewModel = metroViewModel<CoachViewModel>()
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        // Whether Gemini Nano can run here is asked when the coach opens, never in the background.
        LaunchedEffect(viewModel) { viewModel.checkAssistant() }
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        val assistant by viewModel.assistant.collectAsStateWithLifecycle()
        CoachScreen(
            coach = state.coach,
            assistant = assistant,
            today = state.today,
            onChange = viewModel::change,
            onCreate = viewModel::createProgram,
            onSuggest = viewModel::nextSuggestion,
            onReset = viewModel::reset,
            onOpenExercise = openExercise,
            onDownloadAssistant = viewModel::downloadAssistant,
            onWriteMessage = viewModel::writeAssistantMessage,
            contentPadding = contentPadding,
        )
    }
    entry<WorkoutRoute> { route ->
        // Leaving without finishing pops the entry, which clears this ViewModel: the session is discarded.
        val viewModel = assistedMetroViewModel<WorkoutViewModel, WorkoutViewModel.Factory> {
            create(route.dayOfWeek)
        }
        val state by viewModel.state.collectAsStateWithLifecycle()
        WorkoutScreen(
            state = state,
            onNext = viewModel::next,
            onEffort = viewModel::setEffort,
            onPain = viewModel::setPain,
            onFinish = {
                viewModel.finish()
                navigator.goBack()
            },
            onShowHow = openExercise,
            contentPadding = contentPadding,
        )
    }
    entry<ExerciseRoute> { route ->
        ExerciseLibrary.byId(route.exerciseId)?.let { motion ->
            val viewModel = assistedMetroViewModel<ExerciseViewModel, ExerciseViewModel.Factory> {
                create(route.exerciseId)
            }
            val planned by viewModel.planned.collectAsStateWithLifecycle()
            ExerciseDetailScreen(motion = motion, planned = planned, contentPadding = contentPadding)
        }
    }
}

/** The top bar title of a screen inside the Training tab, or null when [route] is not one of them. */
@Composable
fun trainingDetailTitle(route: NavKey?): String? = when (route) {
    is ExerciseRoute -> moveName(route.exerciseId)
    CoachRoute -> stringResource(R.string.coach_title)
    CardioLogRoute -> stringResource(R.string.cardio_log_title)
    ExerciseLibraryRoute -> stringResource(R.string.exercises_title)
    is SessionRoute -> sessionTitle(route.dayOfWeek)
    is WorkoutRoute -> stringResource(R.string.workout_title)
    else -> null
}
