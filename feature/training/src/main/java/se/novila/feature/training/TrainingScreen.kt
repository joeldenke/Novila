package se.novila.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaActionChip
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.ExerciseState
import se.novila.model.CardioLog
import se.novila.movement.MovementState
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData
import java.time.DayOfWeek

/**
 * The Training tab, most useful first: the week at a glance with today's one main action, a few actions that fit
 * right now, the next strength session, the week's program, movement from Health Connect and the exercise library.
 */
@Composable
fun TrainingScreen(
    state: TrainingUiState,
    movement: MovementState,
    actions: TrainingActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    exercise: ExerciseState = ExerciseState.Unavailable,
) {
    val today = state.today
    val plan = remember(state) { state.todayPlan() }
    // Today's session is started from the overview, so the card shows the next one still to do after it.
    val next = state.program.sessions.firstOrNull { session ->
        session.exercises.isNotEmpty() && (session.day > today || (session.day == today && plan is TodayPlan.Workout))
    }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "overview") {
            NovilaReveal(order = 0) {
                TrainingOverview(
                    program = state.program,
                    today = today,
                    weekDone = state.weekDone,
                    plan = plan,
                    onStartWorkout = actions.onStartWorkout,
                    onLogCardio = actions.onLogCardio,
                    onOpenSession = actions.onOpenSession,
                )
            }
        }
        item(key = "actions") {
            NovilaReveal(order = 1) { QuickActions(state.coach, movement, actions) }
        }
        if (next != null) {
            item(key = "next-session") {
                NovilaReveal(order = 2) {
                    StrengthSessionCard(
                        session = next,
                        onOpenExercise = actions.onOpenExercise,
                        // Today's session already has the overview's button.
                        onStart = { actions.onStartWorkout(next.day) }.takeIf { next.day != today },
                        next = false,
                    )
                }
            }
        }
        item(key = "program") {
            NovilaReveal(order = 3) {
                TrainingProgramCard(state.program, today, state.weekDone, onOpenSession = actions.onOpenSession)
            }
        }
        item(key = "movement") {
            MovementCard(
                movement,
                actions.onConnectHealth,
                actions.onOpenHealthSettings,
                actions.onInstallHealth,
                exercise,
            )
        }
        item(key = "exercises") {
            ExerciseLibraryCard(actions.onOpenExercise, actions.onOpenLibrary)
        }
    }
}

/** What the Training tab can do, from the entry: navigation, Health Connect and logging. */
data class TrainingActions(
    val onStartWorkout: (DayOfWeek) -> Unit = {},
    val onOpenSession: (DayOfWeek) -> Unit = {},
    val onOpenLibrary: () -> Unit = {},
    val onLogCardio: (CardioLog) -> Unit = {},
    val onOpenCardioLog: () -> Unit = {},
    val onOpenCoach: () -> Unit = {},
    val onOpenExercise: (String) -> Unit = {},
    val onConnectHealth: () -> Unit = {},
    val onOpenHealthSettings: () -> Unit = {},
    val onInstallHealth: () -> Unit = {},
)

/**
 * Small actions that fit right now: log a workout that wasn't planned, set up or adjust the coach, and connect
 * Health Connect while it isn't.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActions(coach: CoachUiState, movement: MovementState, actions: TrainingActions) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
    ) {
        NovilaActionChip(stringResource(R.string.overview_log_other), onClick = actions.onOpenCardioLog)
        NovilaActionChip(stringResource(R.string.library_search_chip), onClick = actions.onOpenLibrary)
        NovilaActionChip(
            stringResource(if (coach.active) R.string.coach_adjust else R.string.coach_open),
            onClick = actions.onOpenCoach,
        )
        if (movement is MovementState.NeedsPermission && !movement.denied) {
            NovilaActionChip(stringResource(R.string.movement_connect), onClick = actions.onConnectHealth)
        }
    }
}

@Composable
private fun TrainingScreenContent(
    state: TrainingUiState,
    movement: MovementState = PreviewData.movement,
    exercise: ExerciseState = ExerciseState.Unavailable,
) {
    NovilaTheme {
        TrainingScreen(
            state = state,
            movement = movement,
            actions = TrainingActions(),
            contentPadding = PaddingValues(),
            exercise = exercise,
        )
    }
}

/** Thursday, a strength day, with Monday's strength and Tuesday's run done. */
private val strengthDay = TrainingUiState(
    program = TrainingPreviewData.program,
    coach = TrainingPreviewData.newCoach,
    today = DayOfWeek.THURSDAY,
    strengthMinutesToday = 0,
    weekDone = mapOf(DayOfWeek.MONDAY to 28, DayOfWeek.TUESDAY to 32),
)

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun TrainingScreenPreview() = TrainingScreenContent(
    strengthDay,
    exercise = ExerciseState.Connected(TrainingPreviewData.workouts),
)

/** Friday's run, not logged yet: one tap logs it. */
@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun TrainingScreenCardioPreview() = TrainingScreenContent(
    strengthDay.copy(today = DayOfWeek.FRIDAY, weekDone = strengthDay.weekDone + (DayOfWeek.THURSDAY to 30)),
    exercise = ExerciseState.NeedsPermission(),
)

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun TrainingScreenConnectPreview() =
    TrainingScreenContent(strengthDay.copy(today = DayOfWeek.SATURDAY), MovementState.NeedsPermission())
