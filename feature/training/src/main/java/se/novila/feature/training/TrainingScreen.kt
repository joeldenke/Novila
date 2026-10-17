package se.novila.feature.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.ExerciseState
import se.novila.exercise.TrainingProgram
import se.novila.model.CardioLog
import se.novila.movement.MovementState
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData
import java.time.DayOfWeek

@Composable
fun TrainingScreen(
    movement: MovementState,
    program: TrainingProgram,
    coach: CoachUiState,
    today: DayOfWeek,
    onConnectHealth: () -> Unit,
    onOpenHealthSettings: () -> Unit,
    onInstallHealth: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onStartWorkout: (DayOfWeek) -> Unit,
    onOpenCoach: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    strengthMinutesToday: Int = 0,
    cardioToday: CardioToday = CardioToday(),
    exercise: ExerciseState = ExerciseState.Unavailable,
    onLogCardio: (CardioLog) -> Unit = {},
    onUndoCardio: () -> Unit = {},
) {
    val strengthDays = program.sessions.filter { it.exercises.isNotEmpty() }
    // The sun goes to the next strength day still to do: today's, unless it's done, or a later one.
    val next = strengthDays.firstOrNull { it.day > today || (it.day == today && strengthMinutesToday == 0) }
    val plannedCardio = program.sessions.firstOrNull { it.day == today && it.type.cardioType != null }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "movement") {
            MovementCard(movement, onConnectHealth, onOpenHealthSettings, onInstallHealth, exercise)
        }
        item(key = "program") {
            TrainingProgramCard(program, today)
        }
        item(key = "cardio") {
            CardioCard(cardioToday, plannedCardio, onLog = onLogCardio, onUndo = onUndoCardio)
        }
        item(key = "coach") {
            CoachCard(coach, onOpen = onOpenCoach)
        }
        strengthDays.forEach { session ->
            item(key = "strength-${session.day}") {
                StrengthSessionCard(
                    session = session,
                    onOpenExercise = onOpenExercise,
                    onStart = { onStartWorkout(session.day) },
                    next = session == next,
                    minutesDone = strengthMinutesToday.takeIf { session.day == today && it > 0 },
                )
            }
        }
        item(key = "exercises") {
            ExerciseLibraryCard(onOpenExercise)
        }
    }
}

@Composable
private fun TrainingScreenContent(
    movement: MovementState,
    exercise: ExerciseState = ExerciseState.Unavailable,
    cardioToday: CardioToday = CardioToday(),
) {
    NovilaTheme {
        TrainingScreen(
            movement = movement,
            program = TrainingPreviewData.program,
            coach = TrainingPreviewData.newCoach,
            today = PreviewData.today.date.dayOfWeek,
            onConnectHealth = {},
            onOpenHealthSettings = {},
            onInstallHealth = {},
            onOpenExercise = {},
            onStartWorkout = {},
            onOpenCoach = {},
            contentPadding = PaddingValues(),
            cardioToday = cardioToday,
            exercise = exercise,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainingScreenPreview() = TrainingScreenContent(
    PreviewData.movement,
    ExerciseState.Connected(TrainingPreviewData.workouts),
    CardioToday(recordedMinutes = 32),
)

@Preview(showBackground = true)
@Composable
private fun TrainingScreenWorkoutsPreview() =
    TrainingScreenContent(PreviewData.movement, ExerciseState.NeedsPermission())

@Preview(showBackground = true)
@Composable
private fun TrainingScreenConnectPreview() = TrainingScreenContent(MovementState.NeedsPermission())
