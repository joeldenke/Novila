package se.novila.feature.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.TrainingProgram
import se.novila.movement.MovementState
import se.novila.ui.common.ExerciseLibraryCard
import se.novila.ui.common.MovementCard
import se.novila.ui.common.ScreenList
import se.novila.ui.common.StrengthSessionCard
import se.novila.ui.common.TrainingProgramCard
import se.novila.ui.preview.PreviewData
import java.time.DayOfWeek

@Composable
fun TrainingScreen(
    movement: MovementState,
    program: TrainingProgram,
    today: DayOfWeek,
    onConnectHealth: () -> Unit,
    onOpenHealthSettings: () -> Unit,
    onInstallHealth: () -> Unit,
    onOpenExercise: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "movement") {
            MovementCard(movement, onConnectHealth, onOpenHealthSettings, onInstallHealth)
        }
        item(key = "program") {
            TrainingProgramCard(program, today)
        }
        program.sessions.filter { it.exercises.isNotEmpty() }.forEach { session ->
            item(key = "strength-${session.day}") { StrengthSessionCard(session, onOpenExercise) }
        }
        item(key = "exercises") {
            ExerciseLibraryCard(onOpenExercise)
        }
    }
}

@Composable
private fun TrainingScreenContent(movement: MovementState) {
    NovilaTheme {
        TrainingScreen(
            movement = movement,
            program = PreviewData.program,
            today = PreviewData.today.date.dayOfWeek,
            onConnectHealth = {},
            onOpenHealthSettings = {},
            onInstallHealth = {},
            onOpenExercise = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TrainingScreenPreview() = TrainingScreenContent(PreviewData.movement)

@Preview(showBackground = true)
@Composable
private fun TrainingScreenConnectPreview() = TrainingScreenContent(MovementState.NeedsPermission())
