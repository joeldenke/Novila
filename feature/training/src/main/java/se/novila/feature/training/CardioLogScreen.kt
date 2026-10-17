package se.novila.feature.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaScaffold
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.CardioLog
import se.novila.model.CardioType
import se.novila.ui.common.ScreenList
import java.time.DayOfWeek

/**
 * Logs a walk, run, ride or swim by hand, planned or not, and takes it back again. Starts from today's planned
 * cardio, when there is one.
 */
@Composable
fun CardioLogScreen(
    state: TrainingUiState,
    onLog: (CardioLog) -> Unit,
    onUndo: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val planned = state.program.sessions.firstOrNull { it.day == state.today && it.type.cardioType != null }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "cardio") { CardioCard(state.cardioToday, planned, onLog = onLog, onUndo = onUndo) }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardioLogScreenPreview() {
    NovilaTheme {
        NovilaScaffold { padding ->
            CardioLogScreen(
                state = TrainingUiState(
                    program = TrainingPreviewData.program,
                    coach = TrainingPreviewData.newCoach,
                    today = DayOfWeek.TUESDAY,
                    strengthMinutesToday = 0,
                    cardioToday = CardioToday(logged = listOf(CardioLog(CardioType.Walking, 20))),
                ),
                onLog = {},
                onUndo = {},
                contentPadding = padding,
            )
        }
    }
}
