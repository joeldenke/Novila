package se.novila.feature.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.TrainingProgram
import se.novila.movement.MovementState
import se.novila.ui.common.MovementCard
import se.novila.ui.common.ScreenList
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
