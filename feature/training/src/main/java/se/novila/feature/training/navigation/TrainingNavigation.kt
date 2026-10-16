package se.novila.feature.training.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import se.novila.anatomy.ExerciseLibrary
import se.novila.coach.CoachPreferences
import se.novila.coach.CoachPromptInput
import se.novila.exercise.TrainingProgram
import se.novila.feature.training.AssistantUiState
import se.novila.feature.training.CoachScreen
import se.novila.feature.training.CoachUiState
import se.novila.feature.training.ExerciseDetailScreen
import se.novila.feature.training.R
import se.novila.feature.training.TrainingScreen
import se.novila.feature.training.WorkoutScreen
import se.novila.feature.training.WorkoutUiState
import se.novila.feature.training.moveName
import se.novila.movement.MovementState
import se.novila.navigation.Navigator
import java.time.DayOfWeek

/** The Training tab: movement, the week's program, the coach and the exercise library. */
@Serializable data object TrainingRoute : NavKey

/** How to do one exercise from the anatomy library. */
@Serializable data class ExerciseRoute(val exerciseId: String) : NavKey

/** The training coach: answers in, a week's program out. */
@Serializable data object CoachRoute : NavKey

/** A guided strength session, set by set, from a strength day in the Training tab. */
@Serializable data object WorkoutRoute : NavKey

/** What the training screens show. */
data class TrainingState(
    val movement: MovementState,
    val program: TrainingProgram,
    val coach: CoachUiState,
    val assistant: AssistantUiState,
    val workout: WorkoutUiState?,
    val today: DayOfWeek,
    val strengthMinutesToday: Int,
)

/**
 * The Training tab and the screens opened from it. Navigation inside the tab goes through [navigator]; Health
 * Connect and the data behind the screens come from the app through the callbacks.
 */
fun EntryProviderScope<NavKey>.trainingEntries(
    state: TrainingState,
    navigator: Navigator,
    contentPadding: PaddingValues,
    onConnectHealth: () -> Unit,
    onOpenHealthSettings: () -> Unit,
    onInstallHealth: () -> Unit,
    onCheckAssistant: () -> Unit,
    onDownloadAssistant: () -> Unit,
    onWriteAssistantMessage: (CoachPromptInput) -> Unit,
    onChangeCoach: (CoachPreferences) -> Unit,
    onCreateCoachProgram: () -> Unit,
    onNextCoachSuggestion: () -> Unit,
    onResetCoach: () -> Unit,
    onStartWorkout: (DayOfWeek) -> Unit,
    onNextWorkoutStep: () -> Unit,
    onFinishWorkout: () -> Unit,
) {
    val openExercise = { id: String -> navigator.navigate(ExerciseRoute(id)) }
    entry<TrainingRoute> {
        TrainingScreen(
            movement = state.movement,
            program = state.program,
            coach = state.coach,
            today = state.today,
            onConnectHealth = onConnectHealth,
            onOpenHealthSettings = onOpenHealthSettings,
            onInstallHealth = onInstallHealth,
            onOpenExercise = openExercise,
            onStartWorkout = { day ->
                onStartWorkout(day)
                navigator.navigate(WorkoutRoute)
            },
            onOpenCoach = { navigator.navigate(CoachRoute) },
            contentPadding = contentPadding,
            strengthMinutesToday = state.strengthMinutesToday,
        )
    }
    entry<CoachRoute> {
        // Whether Gemini Nano can run here is asked when the coach opens, never in the background.
        LaunchedEffect(Unit) { onCheckAssistant() }
        CoachScreen(
            coach = state.coach,
            assistant = state.assistant,
            today = state.today,
            onChange = onChangeCoach,
            onCreate = onCreateCoachProgram,
            onSuggest = onNextCoachSuggestion,
            onReset = onResetCoach,
            onOpenExercise = openExercise,
            onDownloadAssistant = onDownloadAssistant,
            onWriteMessage = onWriteAssistantMessage,
            contentPadding = contentPadding,
        )
    }
    entry<WorkoutRoute> {
        WorkoutScreen(
            state = state.workout,
            onNext = onNextWorkoutStep,
            onFinish = {
                onFinishWorkout()
                navigator.goBack()
            },
            onShowHow = openExercise,
            contentPadding = contentPadding,
        )
    }
    entry<ExerciseRoute> { route ->
        ExerciseLibrary.byId(route.exerciseId)?.let { motion ->
            ExerciseDetailScreen(
                motion = motion,
                planned = state.program.sessions.flatMap { it.exercises }
                    .firstOrNull { it.exerciseId == route.exerciseId },
                contentPadding = contentPadding,
            )
        }
    }
}

/** The top bar title of a screen inside the Training tab, or null when [route] is not one of them. */
@Composable
fun trainingDetailTitle(route: NavKey?): String? = when (route) {
    is ExerciseRoute -> moveName(route.exerciseId)
    CoachRoute -> stringResource(R.string.coach_title)
    WorkoutRoute -> stringResource(R.string.workout_title)
    else -> null
}
