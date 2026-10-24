package se.novila.feature.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaBottomSheet
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaEntries
import se.novila.designsystem.component.NovilaEntry
import se.novila.designsystem.component.NovilaEntryLayout
import se.novila.designsystem.component.NovilaQuickActions
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaSky
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.ExerciseState
import se.novila.model.CardioLog
import se.novila.model.CycleDay
import se.novila.model.CycleSymptom
import se.novila.movement.MovementState
import se.novila.navigation.SharedScreen
import se.novila.recovery.RecoveryState
import se.novila.recovery.RecoveryStatus
import se.novila.ui.common.ScreenList
import se.novila.ui.common.durationText
import se.novila.ui.common.setupHint
import se.novila.ui.preview.PreviewData
import java.time.DayOfWeek
import se.novila.ui.R as UiR

/**
 * The Training tab: the week at a glance with today's one main action, and movement and recovery from the watch
 * (today's session may be lighter for it) right below once Health Connect is connected. Then the card that logs
 * another workout and the ways into the week's program (with the weekly review), the coach and the exercise library.
 * Before Health Connect is connected, the card with its numbers waits at the bottom. For a user who follows their
 * cycle, the log card also opens the cycle sheet, and on a day with symptoms the coach offers a lighter session right
 * under the week, which the user takes or leaves ([openCycleSheet] at first). When the introduction
 * was skipped ([setupIncomplete]), a quiet card at the top leads to finishing the setup (actions.onCompleteSetup).
 */
@Composable
fun TrainingScreen(
    state: TrainingUiState,
    movement: MovementState,
    actions: TrainingActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    exercise: ExerciseState = ExerciseState.Unavailable,
    recovery: RecoveryState = RecoveryState.Unavailable,
    sky: NovilaSky = NovilaSky.Day,
    /** The introduction was skipped, so the coach's answers are not set up: one quiet card offers to finish it. */
    setupIncomplete: Boolean = false,
    openCycleSheet: Boolean = false,
) {
    val today = state.today
    val plan = remember(state) { state.todayPlan() }
    var loggingCycle by rememberSaveable(openCycleSheet) { mutableStateOf(openCycleSheet) }
    val cycle = state.cycle
    val connected = movement is MovementState.Connected
    ScreenList(
        contentPadding = contentPadding,
        modifier = modifier,
        aside = {
            item(key = "entries") {
                NovilaReveal(order = 2) { NovilaEntries(trainingHubEntries(state, actions)) }
            }
            if (!connected) healthData(movement, recovery, exercise, state, actions)
        },
    ) {
        setupHint(setupIncomplete, actions.onCompleteSetup)
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
                    sky = sky,
                )
            }
        }
        if (cycle?.offerLighter == true) {
            item(key = "lighter") {
                NovilaReveal(order = 1) { LighterSessionCard(onChoose = actions.cycle.onChooseSession) }
            }
        }
        // Connected, the watch's numbers belong with today's plan; until then the card waits at the bottom.
        if (connected) healthData(movement, recovery, exercise, state, actions)
        item(key = "log") {
            NovilaReveal(order = 1) {
                NovilaQuickActions(
                    title = stringResource(R.string.training_log_title),
                    icon = painterResource(UiR.drawable.ic_training),
                    role = NovilaTheme.pillars.training,
                ) {
                    NovilaButton(
                        text = stringResource(R.string.overview_log_other),
                        onClick = actions.onOpenCardioLog,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    if (cycle != null) {
                        NovilaButton(
                            text = stringResource(R.string.cycle_log),
                            onClick = { loggingCycle = true },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
    if (loggingCycle && cycle != null) {
        NovilaBottomSheet(title = stringResource(R.string.cycle_sheet_title), onDismiss = { loggingCycle = false }) {
            CycleSheet(cycle.day, actions.cycle)
        }
    }
}

/** Movement and recovery from Health Connect, or how to connect it. */
private fun LazyListScope.healthData(
    movement: MovementState,
    recovery: RecoveryState,
    exercise: ExerciseState,
    state: TrainingUiState,
    actions: TrainingActions,
) {
    item(key = "health") {
        HealthDataCard(
            movement = movement,
            recovery = recovery,
            exercise = exercise,
            status = state.recovery,
            actions = HealthDataActions(
                onConnect = actions.onConnectHealth,
                onConnectRecovery = actions.onConnectRecovery,
                onOpenSettings = actions.onOpenHealthSettings,
                onInstall = actions.onInstallHealth,
            ),
        )
    }
}

/** The week's program, the coach, the training over time and the exercise library, each with where it stands. */
@Composable
private fun trainingHubEntries(state: TrainingUiState, actions: TrainingActions): List<NovilaEntry> {
    val training = NovilaTheme.pillars.training
    return listOf(
        NovilaEntry(
            title = stringResource(R.string.program_title),
            icon = painterResource(R.drawable.ic_program),
            role = training,
            onClick = actions.onOpenProgram,
            sharedKey = SharedScreen.TrainingProgram,
            summary = stringResource(R.string.training_entry_program_summary, state.program.sessions.size),
        ),
        NovilaEntry(
            title = stringResource(R.string.training_entry_coach),
            icon = painterResource(UiR.drawable.ic_insight),
            role = training,
            onClick = actions.onOpenCoach,
            sharedKey = SharedScreen.TrainingCoach,
            summary = stringResource(
                if (state.coach.active) R.string.training_entry_coach_on else R.string.training_entry_coach_off,
            ),
        ),
        NovilaEntry(
            title = stringResource(R.string.training_entry_trends),
            icon = painterResource(UiR.drawable.ic_insight),
            role = training,
            onClick = actions.onOpenTrends,
            sharedKey = SharedScreen.TrainingTrends,
            summary = stringResource(R.string.training_entry_trends_summary, durationText(state.weekDone.values.sum())),
        ),
        NovilaEntry(
            title = stringResource(R.string.exercises_title),
            icon = painterResource(UiR.drawable.ic_training),
            role = training,
            onClick = actions.onOpenLibrary,
            sharedKey = SharedScreen.ExerciseLibrary,
            summary = stringResource(R.string.exercises_body),
        ),
    )
}

/** What the Training tab can do, from the entry: navigation, Health Connect and logging. */
data class TrainingActions(
    val onStartWorkout: (DayOfWeek) -> Unit = {},
    val onOpenSession: (DayOfWeek) -> Unit = {},
    val onOpenLibrary: () -> Unit = {},
    val onLogCardio: (CardioLog) -> Unit = {},
    val onOpenCardioLog: () -> Unit = {},
    val onOpenCoach: () -> Unit = {},
    val onOpenProgram: () -> Unit = {},
    val onOpenTrends: () -> Unit = {},
    val onOpenExercise: (String) -> Unit = {},
    val onConnectHealth: () -> Unit = {},
    val onConnectRecovery: () -> Unit = {},
    val onOpenHealthSettings: () -> Unit = {},
    val onInstallHealth: () -> Unit = {},
    /** Reopens the intro, for the card shown when the setup was skipped. */
    val onCompleteSetup: () -> Unit = {},
    /** The cycle sheet and the offer of a lighter session, for a user who follows their cycle. */
    val cycle: CycleActions = CycleActions(),
)

@Composable
private fun TrainingScreenContent(
    state: TrainingUiState,
    movement: MovementState = PreviewData.movement,
    exercise: ExerciseState = ExerciseState.Unavailable,
    recovery: RecoveryState = RecoveryState.Unavailable,
    sky: NovilaSky = NovilaSky.Day,
    entryLayout: NovilaEntryLayout = NovilaEntryLayout.Grid,
) {
    NovilaTheme(entryLayout = entryLayout) {
        TrainingScreen(
            state = state,
            movement = movement,
            actions = TrainingActions(),
            contentPadding = PaddingValues(),
            exercise = exercise,
            recovery = recovery,
            sky = sky,
        )
    }
}

/** Thursday, a strength day, with Monday's strength and Tuesday's run done: the weekly review counts them. */
private val strengthDay = TrainingUiState(
    program = TrainingPreviewData.program,
    coach = TrainingPreviewData.newCoach,
    today = DayOfWeek.THURSDAY,
    strengthMinutesToday = 0,
    weekDone = mapOf(DayOfWeek.MONDAY to 28, DayOfWeek.TUESDAY to 32),
    nextProgram = TrainingPreviewData.nextProgram,
)

@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun TrainingScreenPreview() = TrainingScreenContent(
    strengthDay,
    exercise = ExerciseState.Connected(TrainingPreviewData.workouts),
)

/** The ways in as rows, as chosen in Settings. */
@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun TrainingScreenRowsPreview() = TrainingScreenContent(
    strengthDay,
    exercise = ExerciseState.Connected(TrainingPreviewData.workouts),
    entryLayout = NovilaEntryLayout.Rows,
)

/** A tablet: the week stays readable in the middle, and the ways in share one row. */
@Preview(showBackground = true, widthDp = 900, heightDp = 1200)
@Composable
private fun TrainingScreenTabletPreview() = TrainingScreenContent(
    strengthDay,
    exercise = ExerciseState.Connected(TrainingPreviewData.workouts),
)

/** Friday's run in the evening, not logged yet: one tap logs it. */
@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun TrainingScreenCardioPreview() = TrainingScreenContent(
    strengthDay.copy(today = DayOfWeek.FRIDAY, weekDone = strengthDay.weekDone + (DayOfWeek.THURSDAY to 30)),
    exercise = ExerciseState.NeedsPermission(),
    sky = NovilaSky.Evening,
)

/** Saturday night, before Health Connect is connected. */
@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun TrainingScreenConnectPreview() = TrainingScreenContent(
    strengthDay.copy(today = DayOfWeek.SATURDAY),
    MovementState.NeedsPermission(),
    sky = NovilaSky.Night,
)

/** Cramps logged this morning in *Follow your cycle*: the coach offers a lighter session, and the log has the cycle. */
@Preview(showBackground = true, heightDp = 1700)
@Composable
private fun TrainingScreenCyclePreview() = TrainingScreenContent(
    strengthDay.copy(cycle = CycleToday(CycleDay(symptoms = setOf(CycleSymptom.Cramps)), offerLighter = true)),
    exercise = ExerciseState.Connected(TrainingPreviewData.workouts),
    sky = NovilaSky.Morning,
)

/** The watch says the body is still recovering in the morning: today's session is a little lighter. */
@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun TrainingScreenRecoveryPreview() = TrainingScreenContent(
    strengthDay.copy(recovery = RecoveryStatus.Low),
    exercise = ExerciseState.Connected(TrainingPreviewData.workouts),
    recovery = TrainingPreviewData.recovery,
    sky = NovilaSky.Morning,
)
